package io.github.gaming32.mckt

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import com.mojang.brigadier.tree.CommandNode
import com.mojang.brigadier.tree.LiteralCommandNode
import io.github.gaming32.mckt.GlobalPalette.DEFAULT_BLOCKSTATES
import io.github.gaming32.mckt.blocks.*
import io.github.gaming32.mckt.commands.*
import io.github.gaming32.mckt.commands.arguments.*
import io.github.gaming32.mckt.commands.commands.BuiltinCommand
import io.github.gaming32.mckt.config.ConfigErrorException
import io.github.gaming32.mckt.config.MotdCreationContext
import io.github.gaming32.mckt.config.ServerConfig
import io.github.gaming32.mckt.config.evalConfigFile
import io.github.gaming32.mckt.data.*
import io.github.gaming32.mckt.items.*
import io.github.gaming32.mckt.objects.BlockPosition
import io.github.gaming32.mckt.objects.BlockState
import io.github.gaming32.mckt.objects.Identifier
import io.github.gaming32.mckt.packet.Packet
import io.github.gaming32.mckt.packet.PacketState
import io.github.gaming32.mckt.packet.login.s2c.LoginDisconnectPacket
import io.github.gaming32.mckt.packet.play.KeepAlivePacket
import io.github.gaming32.mckt.packet.play.PlayPingPacket
import io.github.gaming32.mckt.packet.play.s2c.*
import io.github.gaming32.mckt.packet.sendPacket
import io.github.gaming32.mckt.world.World
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.util.collections.*
import io.ktor.utils.io.*
import io.ktor.utils.io.bits.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.jline.reader.*
import org.jline.utils.AttributedString
import org.jline.utils.AttributedStringBuilder
import org.jline.utils.AttributedStyle
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.set
import kotlin.concurrent.thread
import kotlin.io.path.deleteIfExists
import kotlin.io.path.forEachDirectoryEntry
import kotlin.io.use
import kotlin.system.exitProcess
import kotlin.text.toByteArray
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.DurationUnit

private val LOGGER = getLogger()
private val STYLES = listOf(
    AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN),
    AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW),
    AttributedStyle.DEFAULT.foreground(AttributedStyle.MAGENTA)
)
internal val DEBUG = System.getProperty("mckt.debug").toBoolean()

typealias CustomPacketHandler = suspend (channel: Identifier, client: PlayClient, input: InputStream) -> Unit

class MinecraftServer(
    val useJline: Boolean = false
) {
    var running = true
    private val handshakeJobs = mutableSetOf<Job>()
    @PublishedApi internal val clients = mutableMapOf<String, PlayClient>()
    private lateinit var handleCommandsJob: Job
    private lateinit var acceptConnectionsJob: Job

    val configFile = File("config.mckt.kts")
    var config: ServerConfig = ServerConfig.PreConfig
        private set

    lateinit var world: World
        private set
    internal var nextEntityId = 1

    private val consoleCommandSender = ConsoleCommandSource(this, "CONSOLE")
    val serverCommandSender = ConsoleCommandSource(this, "Server")
    val commandDispatcher = CommandDispatcher<CommandSource>()
    internal val helpTexts = mutableMapOf<CommandNode<CommandSource>, Component?>()

    internal val customPacketHandlers = mutableMapOf<Identifier, MutableList<CustomPacketHandler>>()
    internal val blockHandlers = mutableMapOf<Identifier, BlockHandler>()
    internal val itemHandlers = mutableMapOf<Identifier, ItemHandler>()

    @OptIn(DelicateCoroutinesApi::class)
    internal val threadPoolContext = if (Runtime.getRuntime().availableProcessors() == 1) {
        Dispatchers.Default
    } else {
        newFixedThreadPoolContext(Runtime.getRuntime().availableProcessors() - 1, "Heavy-Computation")
    }

    val httpClient = HttpClient(CIO) {
        defaultRequest {
            header("User-Agent", "mckt/$MCKT_VERSION")
        }
        install(ContentNegotiation) {
            json()
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 20_000
        }
    }

    private var mainCoroutineScopeInternal: CoroutineScope? = null
    val mainCoroutineScope get() =
        mainCoroutineScopeInternal ?: throw IllegalStateException("MinecraftServer not running!")

    var targetTps = 20
    val targetMspt get() = 1000.0 / targetTps

    private var manualCrash = false

    suspend fun run() = coroutineScope {
        LOGGER.info("Starting server...")

        mainCoroutineScopeInternal = this

        val loadConfigJob = launch { reloadConfig() }

        registerCommands()
        registerCustomPacketHandlers()
        registerBlockHandlers()
        registerItemHandlers()
        LOGGER.info("Prepared builtin event handlers")

        loadConfigJob.join()

        world = World(this@MinecraftServer, "world")

        LOGGER.info("Searching for spawn point...")
        world.getSpawnPoint().let { LOGGER.info("Found spawn point {}", it) }

        handleCommandsJob = launch { handleCommands() }
        acceptConnectionsJob = launch { acceptConnections() }
        yield()
        LOGGER.info("Server started")
        var startTime = System.nanoTime()
        while (running) {
            try {
                tick()
                val endTime = System.nanoTime()
                val tickTime = (endTime - startTime).nanoseconds.toDouble(DurationUnit.MILLISECONDS)
                if (tickTime >= targetMspt * 21) {
                    LOGGER.warn(
                        "Is the server overloaded? Running {} seconds ({} ticks) behind.",
                        (tickTime - targetMspt) / 1000.0, (tickTime - targetMspt) / targetMspt
                    )
                    broadcast(KeepAlivePacket(System.currentTimeMillis()))
                }
                startTime = System.nanoTime()
                val sleepTime = targetMspt - tickTime
                if (sleepTime <= 0) {
                    yield()
                } else {
                    delay(sleepTime.toLong())
                    startTime += sleepTime.milliseconds.inWholeNanoseconds
                }
            } catch (e: Exception) {
                running = false
                LOGGER.error("FATAL Exception in ticking", e)
                try {
                    for (client in clients.values) {
                        client.kick(Component.text(e.toString(), NamedTextColor.RED))
                        client.close()
                    }
                } catch (_: Exception) {
                }
                break
            }
        }
        LOGGER.info("Stopping server...")
        handleCommandsJob.cancel()
        acceptConnectionsJob.cancel()
        try {
            for (client in clients.values) {
                client.kick(Component.translatable("multiplayer.disconnect.server_shutdown"))
                client.close()
            }
        } catch (_: Exception) {
        }
        clients.clear()
        handshakeJobs.forEach { it.cancel() }
        world.closeAndLog()
        handshakeJobs.joinAll()
        handshakeJobs.clear()
        @OptIn(ExperimentalCoroutinesApi::class)
        if (threadPoolContext is CloseableCoroutineDispatcher) {
            threadPoolContext.close()
        }
        httpClient.close()

        mainCoroutineScopeInternal = null
        LOGGER.info("Server stopped")
    }

    suspend fun reloadConfig(printError: Boolean = true) {
        if (!configFile.isFile) {
            javaClass.getResourceAsStream("/config.mckt.kts")?.use { inp ->
                configFile.outputStream().use { out ->
                    inp.copyTo(out)
                }
            }
        }
        try {
            config = evalConfigFile(configFile)
        } catch (e: ConfigErrorException) {
            if (printError) {
                LOGGER.error("Failed to read config:\n" + e.message)
            }
            throw e
        }
        LOGGER.info("Loaded config")
    }

    private suspend fun tick() {
        world.meta.time++
        if (world.meta.autosave && world.meta.time % config.autosavePeriod == 0L) {
            mainCoroutineScopeInternal!!.launch { world.saveAndLog() }
            clients.values.forEach(PlayClient::save)
        }
        for (client in clients.values.toList()) {
            if (
                client.ended ||
                client.receiveChannel.isClosedForRead ||
                client.handlePacketsJob?.isCompleted == true
            ) {
                client.handlePacketsJob?.cancelAndJoin()
                LOGGER.info("{} left the game.", client.username)
                clients.remove(client.username)
                client.close()
                broadcastChat(Component.translatable(
                    "multiplayer.player.left",
                    NamedTextColor.YELLOW,
                    Component.text(client.username)
                ))
                broadcast(PlayerListUpdatePacket(
                    PlayerListUpdatePacket.RemovePlayer(client.uuid)
                ))
                broadcast(RemoveEntitiesPacket(client.entityId))
                continue
            }
            client.tick()
            if (world.meta.time % 20 == 0L) {
                client.sendPacket(UpdateTimePacket(world.meta.time))
                client.syncPosition(toOthers = true, toSelf = false)
                if (client.pingId == -1) {
                    client.pingId = client.nextPingId++
                    client.sendPacket(PlayPingPacket(client.pingId))
                    client.pingStart = System.nanoTime()
                }
            }
        }
        syncDirtyBlocks()
        if (manualCrash) {
            throw Exception("Manual server crash")
        }
    }

    private suspend fun syncDirtyBlocks() = coroutineScope {
        val sections = mutableMapOf<BlockPosition, MutableMap<BlockPosition, BlockState>>()
        val blockEntityPackets = mutableListOf<Packet>()
        world.dirtyBlocks.forEach {
            val block = world.getBlock(it)
            sections.computeIfAbsent(it shr 4) { mutableMapOf() }[it and 15] = block
            if (block.hasBlockEntity(this@MinecraftServer)) {
                blockEntityPackets += world.getBlockEntity(it)?.updateNetworkSerialize() ?: return@forEach
            }
        }
        if (world.dirtyBlocks.size < 2 shl 16) {
            world.dirtyBlocks.clear()
        } else {
            // Profiling! I noticed after changing an obscene number of blocks on 1 tick that the server was lagging,
            // so I ran a quick profile and found that the thing lagging out the server was the above clear() call.
            world.dirtyBlocks = ConcurrentSet()
        }
        val jobs = mutableListOf<Job>()
        sections.forEach { (sectionLocation, section) ->
            val packet = if (section.size == 1) {
                SetBlockPacket(sectionLocation shl 4 or section.keys.first(), section.values.first())
            } else {
                SectionMultiSetBlockPacket(sectionLocation, section)
            }
            for (client in clients.values) {
                jobs.add(launch { client.sendPacket(packet) })
            }
        }
        for (client in clients.values) {
            for (packet in blockEntityPackets) {
                jobs.add(launch { client.sendPacket(packet) })
            }
        }
    }

    fun registerCommand(
        description: Component?, command: LiteralArgumentBuilder<CommandSource>
    ): LiteralCommandNode<CommandSource> {
        val registered = commandDispatcher.register(command)
        helpTexts[registered] = description
        return registered
    }

    fun registerCommandAliases(command: String, vararg aliases: String) {
        val commandNode = commandDispatcher.root.getChild(command)
            ?: throw IllegalArgumentException("Command $command must be registered before setting aliases")
        val description = helpTexts[commandNode]
        for (alias in aliases) {
            registerCommand(description, literal<CommandSource>(alias)
                .requires(commandNode.requirement)
                .executes(commandNode.command)
                .redirect(commandNode)
            )
        }
    }

    private fun registerCommands() {
        BuiltinCommand::class.sealedSubclasses.forEach {
            val instance = it.objectInstance!!
            val tree = instance.buildTree()
            registerCommand(instance.helpText, tree)
            if (instance.aliases.isNotEmpty()) {
                registerCommandAliases(tree.literal, *instance.aliases.toTypedArray())
            }
        }
        registerCommand(Component.text("Debug tools"), literal<CommandSource>("debug")
            .requires { it.hasPermission(DebugConsts.DEBUG_OP_LEVEL) }
            .then(literal<CommandSource>("reload-registrations")
                .executesSuspend {
                    source.replyBroadcast(Component.text("Reloading commands..."))
                    commandDispatcher.root.children.clear()
                    registerCommands()
                    clients.values.forEach { it.sendCommandTree() }
                    source.replyBroadcast(Component.text("Reloading handlers..."))
                    customPacketHandlers.clear()
                    registerCustomPacketHandlers()
                    blockHandlers.clear()
                    registerBlockHandlers()
                    itemHandlers.clear()
                    registerItemHandlers()
                    source.replyBroadcast(Component.text("Reloaded", NamedTextColor.GREEN))
                    0
                }
            )
            .then(literal<CommandSource>("regenerate-world")
                .executesSuspend {
                    source.replyBroadcast(Component.text("Regenerating world..."))
                    val autosave = world.meta.autosave
                    world.meta.autosave = false
                    clients.values.forEach {
                        it.loadedChunks.forEach { (x, z) ->
                            it.sendPacket(UnloadChunkPacket(x, z))
                        }
                        it.loadedChunks.clear()
                    }
                    world.regionsDir.toPath().forEachDirectoryEntry("region_*_*.nbt") {
                        it.deleteIfExists()
                    }
                    world.openRegions.clear()
                    delay(500)
                    coroutineScope {
                        clients.values.forEach { it.apply {
                            loadChunksAroundPlayer(3)
                        } }
                    }
                    world.meta.autosave = autosave
                    source.replyBroadcast(Component.text("Regenerated world", NamedTextColor.GREEN))
                    0
                }
            )
            .then(literal<CommandSource>("palette-info")
                .then(argument<CommandSource, PositionArgument>("pos", BlockPositionArgumentType)
                    .executesSuspend {
                        val pos = getLoadedBlockPosition("pos")
                        val palette = world
                            .getChunk(pos.x shr 4, pos.z shr 4)
                            ?.getSection(pos.y shr 4)
                            ?.data?.paletteItems?.toSet() ?: setOf()
                        source.reply(Component.text("The section has a palette of size ${palette.size}:"))
                        palette.forEach {
                            source.reply(Component.text("  + $it"))
                        }
                        palette.size
                    }
                )
            )
            .then(literal<CommandSource>("crash")
                .executesSuspend {
                    manualCrash = true
                    0
                }
            )
        )
    }

    fun registerCustomPacketHandler(channel: Identifier, handler: CustomPacketHandler) {
        customPacketHandlers.computeIfAbsent(channel) { mutableListOf() }.add(handler)
    }

    fun registerCustomPacketHandler(channel: String, handler: CustomPacketHandler) =
        registerCustomPacketHandler(Identifier.parse(channel), handler)

    fun getCustomPacketHandlers(channel: Identifier): List<CustomPacketHandler> =
        customPacketHandlers[channel] ?: listOf()

    private fun registerCustomPacketHandlers() {
        registerCustomPacketHandler("brand") { _, client, input ->
            val brand = input.readString()
            client.brand = brand
            if (client.hasFabricApi && brand == "vanilla") {
                val message = "${client.username} says their client is Vanilla, but they have Fabric API installed."
                LOGGER.warn(message)
                if (config.enableVanillaClientSpoofAlerts) {
                    broadcast(SystemChatPacket(
                        Component.text(message, NamedTextColor.YELLOW)
                    )) { it.data.operatorLevel >= 2 }
                }
            }
        }
        registerCustomPacketHandler("register") { _, client, input ->
            client.supportedChannels.addAll(
                input.readBytes()
                    .toString(Charsets.US_ASCII)
                    .split(0.toChar())
                    .asSequence()
                    .map(Identifier::parse)
            )
        }
        registerCustomPacketHandler("unregister") { _, client, input ->
            client.supportedChannels.removeAll(
                input.readBytes()
                    .toString(Charsets.US_ASCII)
                    .split(0.toChar())
                    .asSequence()
                    .map(Identifier::parse)
                    .toSet()
            )
        }
        registerCustomPacketHandler("worldedit:cui") { _, client, input ->
            LOGGER.info("CUI Packet from ${client.username}: ${input.readBytes().toString(Charsets.US_ASCII)}")
        }
    }

    fun registerBlockHandler(handler: BlockHandler, vararg blocks: Identifier) {
        blocks.forEach { blockHandlers[it] = handler }
    }

    fun getBlockHandler(block: Identifier?) = blockHandlers[block] ?: DefaultBlockHandler

    private fun registerBlockHandlers() {
        BLOCK_PROPERTIES.entries.forEach { (id, properties) ->
            when (val type = properties.typeProperties) {
                is BlockTypeProperties.DefaultBlockType -> {}
                is BlockTypeProperties.DoorBlockType -> registerBlockHandler(DoorBlockHandler, id)
                is BlockTypeProperties.PaneBlockType -> registerBlockHandler(PaneBlockHandler, id)
                is BlockTypeProperties.PillarBlockType -> registerBlockHandler(PillarBlockHandler, id)
                is BlockTypeProperties.SaplingBlockType -> registerBlockHandler(SaplingBlockHandler, id)
                is BlockTypeProperties.SlabBlockType -> registerBlockHandler(SlabBlockHandler, id)
                is BlockTypeProperties.StairsBlockType ->
                    registerBlockHandler(StairsBlockHandler(id, type.baseBlockState), id)
                is BlockTypeProperties.TrapdoorBlockType -> registerBlockHandler(TrapdoorBlockHandler, id)
                is BlockTypeProperties.SignBlockType -> registerBlockHandler(SignBlockHandler, id)
                is BlockTypeProperties.WallSignBlockType -> registerBlockHandler(WallSignBlockHandler, id)
            }
        }
    }

    fun registerItemHandler(handler: ItemHandler, vararg items: Identifier) {
        items.forEach { itemHandlers[it] = handler }
    }

    fun getItemHandler(item: Identifier?) = itemHandlers[item] ?: DefaultItemHandler

    private fun registerItemHandlers() {
        ITEM_ID_TO_PROTOCOL.keys.forEach { itemId ->
            if (itemId.value.endsWith("_sword")) {
                registerItemHandler(SwordItemHandler, itemId)
            } else if (itemId.value.endsWith("_sign")) {
                val handler = SignItemHandler(itemId, Identifier(
                    itemId.namespace,
                    itemId.value.substring(0, itemId.value.length - 5) + "_wall_sign"
                ))
                registerItemHandler(handler, itemId)
            } else if (itemId in DEFAULT_BLOCKSTATES) {
                registerItemHandler(BlockItemHandler(itemId), itemId)
            }
        }
        registerItemHandler(BoneMealItemHandler, Identifier("bone_meal"))
        registerItemHandler(DebugStickItemHandler, Identifier("debug_stick"))
        registerItemHandler(FireworkRocketHandler, Identifier("firework_rocket"))
        registerItemHandler(FlintAndSteelHandler, Identifier("flint_and_steel"))
        registerItemHandler(WorldeditItem, Identifier("wooden_axe"))
    }

    fun getPlayerByName(name: String) = clients[name]

    fun getPlayerByUuid(uuid: UUID) = clients.values.firstOrNull { it.uuid == uuid }

    inline fun getPlayers(predicate: (PlayClient) -> Boolean) = clients.values.filter(predicate)

    suspend fun waitTicks(ticks: Int = 1) {
        val target = world.meta.time + ticks
        while (world.meta.time < target) {
            yield()
        }
    }

    @Suppress("RedundantAsync")
    @OptIn(DelicateCoroutinesApi::class)
    private suspend fun handleCommands() {
        if (useJline) {
            val reader = LineReaderBuilder.builder()
                .completer { _, line, candidates ->
                    val parsed = commandDispatcher.parse(line.line(), consoleCommandSender)
                    val suggestions = commandDispatcher.getCompletionSuggestions(parsed, line.cursor()).get()
                    suggestions.list.mapTo(candidates) { Candidate(it.text) }
                }
                .highlighter(object : Highlighter {
                    override fun setErrorIndex(errorIndex: Int) = Unit
                    override fun setErrorPattern(errorPattern: Pattern?) = Unit
                    override fun highlight(reader: LineReader, buffer: String): AttributedString {
                        val parsed = commandDispatcher.parse(buffer, consoleCommandSender)
                        val sb = AttributedStringBuilder()
                        var end = 0
                        parsed.context.nodes.forEachIndexed { index, node ->
                            val range = node.range
                            sb.append(" ".repeat(range.start - end), AttributedStyle.DEFAULT)
                            sb.append(
                                buffer.substring(range.start, range.end.coerceAtMost(buffer.length)),
                                if (node.node is LiteralCommandNode<*>) {
                                    AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN)
                                } else {
                                    STYLES[index % STYLES.size]
                                }
                            )
                            end = range.end
                        }
                        sb.append(
                            buffer.substring(end.coerceAtMost(buffer.length)),
                            AttributedStyle.DEFAULT.foreground(AttributedStyle.RED)
                        )
                        return sb.toAttributedString()
                    }
                })
                .build()
            while (running) {
                val line = try {
                    GlobalScope.async(Dispatchers.IO) {
                        reader.readLine()
                    }.await()?.trim()?.ifEmpty { null } ?: continue
                } catch (e: UserInterruptException) {
                    running = false
                    break
                } catch (e: EndOfFileException) {
                    break
                }
                consoleCommandSender.runCommand(line, commandDispatcher)
            }
        }
        while (running) {
            consoleCommandSender.runCommand(
                GlobalScope.async(Dispatchers.IO) { readlnOrNull() }.await()?.trim()?.ifEmpty { null } ?: continue,
                commandDispatcher
            )
        }
    }

    private suspend fun acceptConnections() = coroutineScope {
        val selectorManager = SelectorManager(Dispatchers.IO)
        aSocket(selectorManager).tcp().bind("0.0.0.0", 25565).use { serverSocket ->
            LOGGER.info("Listening on {}", serverSocket.localAddress)
            while (running) {
                val socket = serverSocket.accept()
                LOGGER.info("Accepted {}", socket.remoteAddress)
                launch initialConnection@ {
                    val receiveChannel = socket.openReadChannel()
                    val sendChannel = socket.openWriteChannel()
                    try {
                        val packetLength = receiveChannel.readVarInt(specialFe = true)
                        if (packetLength == 0xFE) {
                            val isPre14 = receiveChannel.availableForRead == 0
                            val motd = config.motdGenerator(MotdCreationContext(
                                this@MinecraftServer,
                                if (receiveChannel.availableForRead > 1) {
                                    // 1.6
                                    receiveChannel.discard(28)
                                    PingInfo(
                                        receiveChannel.readByte().toUByte().toInt(),
                                        receiveChannel.readPacket(
                                            receiveChannel.readShort().toInt() * 2
                                        ).readText(Charsets.UTF_16BE),
                                        receiveChannel.readInt(),
                                        socket.remoteAddress
                                    )
                                } else {
                                    // Pre-1.6
                                    PingInfo(0, "", 0, socket.remoteAddress)
                                }
                            ))
                            val encoded = if (isPre14) {
                                // Pre-1.4
                                "${motd.plainText().substringBefore('\n')}\u00a7${clients.size}" +
                                    "\u00a7${config.maxPlayers}"
                            } else {
                                // 1.4 through 1.6
                                "\u00a71\u0000127\u0000$MINECRAFT_VERSION\u0000" +
                                    LegacyComponentSerializer.legacySection().serialize(motd).substringBefore('\n') +
                                    "\u0000${clients.size}\u0000${config.maxPlayers}"
                            }.toByteArray(Charsets.UTF_16BE)
                            sendChannel.writeByte(0xff)
                            sendChannel.writeShort(encoded.size / 2)
                            sendChannel.writeFully(encoded, 0, encoded.size)
                            sendChannel.flush()
                            socket.dispose()
                            return@initialConnection
                        }
                        val bytesRead = receiveChannel.totalBytesRead
                        val packetId = receiveChannel.readVarInt()
                        val packetIdLength = (receiveChannel.totalBytesRead - bytesRead).toInt()
                        if (packetId != 0x00) {
                            socket.dispose()
                            return@initialConnection
                        }
                        val packet = ByteArray(packetLength - packetIdLength)
                        receiveChannel.readFully(packet, 0, packet.size)
                        val packetInput = ByteArrayInputStream(packet)
                        val protocolVersion = packetInput.readVarInt()
                        val serverIp = packetInput.readString(255) // Server address
                        val serverPort = packetInput.readShort() // Server port
                        val nextStateInt = packetInput.readVarInt()
                        val nextState = try {
                            PacketState.values()[nextStateInt]
                        } catch (e: IndexOutOfBoundsException) {
                            LOGGER.warn("Unexpected packet state: ${nextStateInt.toString(16)}")
                            socket.dispose()
                            return@initialConnection
                        }
                        val pingInfo = PingInfo(
                            protocolVersion,
                            serverIp, serverPort.toUShort().toInt(),
                            socket.remoteAddress
                        )
                        when (nextState) {
                            PacketState.STATUS -> handshakeJobs.add(launch {
                                try {
                                    StatusClient(
                                        this@MinecraftServer, socket, receiveChannel, sendChannel
                                    ).handle(pingInfo)
                                } catch (e: Exception) {
                                    if (e !is ClosedReceiveChannelException) {
                                        LOGGER.error("Error sharing status with client", e)
                                    }
                                } finally {
                                    handshakeJobs.remove(coroutineContext[Job])
                                }
                            })
                            PacketState.LOGIN -> handshakeJobs.add(launch {
                                if (protocolVersion != PROTOCOL_VERSION) {
                                    val message = "Unsupported game version: " +
                                        GAME_VERSIONS_BY_PROTOCOL[protocolVersion].let { version ->
                                            if (version != null) {
                                                "${version.minecraftVersion} (protocol version $protocolVersion)"
                                            } else {
                                                "Protocol version $protocolVersion"
                                            }
                                        }
                                    LOGGER.warn(message)
                                    sendChannel.sendPacket(LoginDisconnectPacket(Component.text(message)), -1)
                                    socket.dispose()
                                    return@launch
                                }
                                val client = PlayClient(this@MinecraftServer, socket, receiveChannel, sendChannel)
                                try {
                                    client.handshake()
                                } finally {
                                    handshakeJobs.remove(coroutineContext[Job])
                                }
                                if (!client.receiveChannel.isClosedForRead) {
                                    LOGGER.info("{} joined the game.", client.username)
                                    client.handlePacketsJob = launch { client.handlePackets() }
                                    clients.put(client.username, client)?.also { oldClient ->
                                        if (oldClient.receiveChannel.isClosedForRead) return@also
                                        LOGGER.info("Another client with that username was already online")
                                        oldClient.kick(
                                            Component.translatable("multiplayer.disconnect.duplicate_login")
                                        )
                                        oldClient.save()
                                        broadcastChat(Component.translatable(
                                            "multiplayer.player.left",
                                            NamedTextColor.YELLOW,
                                            Component.text(client.username)
                                        ))
                                    }
                                    broadcastChat(Component.translatable(
                                        "multiplayer.player.joined",
                                        NamedTextColor.YELLOW,
                                        Component.text(client.username)
                                    ))
                                    client.postHandshake(pingInfo)
                                }
                            })
                            else -> {
                                LOGGER.warn("Unexpected packet state: $nextState")
                                socket.dispose()
                            }
                        }
                    } catch (e: Exception) {
                        LOGGER.warn("Client sent invalid data during handshake", e)
                        socket.dispose()
                    }
                }
            }
        }
    }

    suspend fun broadcastChat(message: Component) {
        LOGGER.info("CHAT: {}", if (useJline) message.attributedText().toAnsi() else message.plainText())
        broadcast(SystemChatPacket(message))
    }

    suspend fun broadcast(packet: Packet) = coroutineScope {
        clients.values.map { client ->
            launch { client.sendPacket(packet) }
        }.joinAll()
    }

    suspend inline fun broadcast(packet: Packet, crossinline condition: (PlayClient) -> Boolean) = coroutineScope {
        clients.values.mapNotNull { client ->
            if (condition(client)) launch { client.sendPacket(packet) } else null
        }.joinAll()
    }

    suspend fun broadcastExcept(client: PlayClient?, packet: Packet) = broadcast(packet) { it !== client }
}

fun main(vararg args: String) {
    thread(name = "Timer-Hack-Thread", isDaemon = true) {
        while (true) {
            Thread.sleep(Long.MAX_VALUE)
        }
    }
    try {
        runBlocking { MinecraftServer(useJline = "--no-jline" !in args).run() }
    } catch (_: ConfigErrorException) {
        exitProcess(1)
    }
}
