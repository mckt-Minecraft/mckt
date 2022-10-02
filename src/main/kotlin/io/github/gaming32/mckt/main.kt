package io.github.gaming32.mckt

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import com.mojang.brigadier.tree.CommandNode
import com.mojang.brigadier.tree.LiteralCommandNode
import io.github.gaming32.mckt.GlobalPalette.BLOCK_STATE_PROPERTIES
import io.github.gaming32.mckt.GlobalPalette.DEFAULT_BLOCKSTATES
import io.github.gaming32.mckt.blocks.*
import io.github.gaming32.mckt.commands.*
import io.github.gaming32.mckt.commands.arguments.*
import io.github.gaming32.mckt.commands.commands.BuiltinCommand
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
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.jline.reader.*
import org.jline.utils.AttributedString
import org.jline.utils.AttributedStringBuilder
import org.jline.utils.AttributedStyle
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.set
import kotlin.concurrent.thread
import kotlin.io.path.deleteIfExists
import kotlin.io.path.forEachDirectoryEntry
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds

private val LOGGER = getLogger()
private val STYLES = listOf(
    AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN),
    AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW),
    AttributedStyle.DEFAULT.foreground(AttributedStyle.MAGENTA)
)

typealias CustomPacketHandler = suspend (channel: Identifier, client: PlayClient, input: InputStream) -> Unit

class MinecraftServer(
    val useJline: Boolean = false
) {
    var running = true
    private val handshakeJobs = mutableSetOf<Job>()
    @PublishedApi internal val clients = mutableMapOf<String, PlayClient>()
    private lateinit var handleCommandsJob: Job
    private lateinit var acceptConnectionsJob: Job

    val configFile = File("config.json")
    @OptIn(ExperimentalSerializationApi::class)
    val config = try {
        configFile.inputStream().use { PRETTY_JSON.decodeFromStream(it) }
    } catch (e: Exception) {
        if (e !is FileNotFoundException) {
            LOGGER.warn("Couldn't read server config, creating anew", e)
        }
        ServerConfig()
    }.also { config ->
        configFile.outputStream().use { PRETTY_JSON.encodeToStream(config, it) }
    }

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

    suspend fun run() = coroutineScope {
        LOGGER.info("Starting server...")

        registerCommands()
        registerCustomPacketHandlers()
        registerBlockHandlers()
        registerItemHandlers()

        world = World(this@MinecraftServer, "world")

        LOGGER.info("Searching for spawn point...")
        world.findSpawnPoint().let { LOGGER.info("Found spawn point {}", it) }

        handleCommandsJob = launch { handleCommands() }
        acceptConnectionsJob = launch { acceptConnections() }
        yield()
        LOGGER.info("Server started")
        var startTime = System.nanoTime()
        while (running) {
            world.meta.time++
            if (world.meta.autosave && world.meta.time % config.autosavePeriod == 0L) {
                launch { world.saveAndLog() }
                clients.values.forEach(PlayClient::save)
            }
            for (client in clients.values.toList()) {
                if (client.ended || client.receiveChannel.isClosedForRead || client.handlePacketsJob.isCompleted) {
                    client.handlePacketsJob.cancelAndJoin()
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
            val endTime = System.nanoTime()
            val tickTime = (endTime - startTime).nanoseconds.inWholeMilliseconds
            if (tickTime >= 1050) {
                LOGGER.warn(
                    "Is the server overloaded? Running {} seconds ({} ticks) behind.",
                    (tickTime - 50) / 1000.0, (tickTime - 50) / 50.0
                )
                broadcast(KeepAlivePacket(System.currentTimeMillis()))
            }
            startTime = System.nanoTime()
            val sleepTime = 50 - tickTime
            if (sleepTime <= 0) {
                yield()
            } else {
                delay(sleepTime)
                startTime += sleepTime.milliseconds.inWholeNanoseconds
            }
        }
        LOGGER.info("Stopping server...")
        handleCommandsJob.cancel()
        acceptConnectionsJob.cancel()
        for (client in clients.values) {
            client.kick(Component.translatable("multiplayer.disconnect.server_shutdown"))
            client.close()
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
        LOGGER.info("Server stopped")
    }

    private suspend fun syncDirtyBlocks() = coroutineScope {
        val sections = mutableMapOf<BlockPosition, MutableMap<BlockPosition, BlockState>>()
        world.dirtyBlocks.forEach {
            sections.computeIfAbsent(it shr 4) { mutableMapOf() }[it and 15] = world.getBlock(it)
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
            .requires { it.hasPermission(5) }
            .then(literal<CommandSource>("reload-commands")
                .executesSuspend {
                    source.replyBroadcast(Component.text("Reloading commands..."))
                    commandDispatcher.root.children.clear()
                    registerCommands()
                    clients.values.forEach { it.sendCommandTree() }
                    source.replyBroadcast(Component.text("Reloaded commands", NamedTextColor.GREEN))
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
                            ?.data?.palette?.values ?: setOf()
                        source.reply(Component.text("The section has a palette of size ${palette.size}:"))
                        palette.forEach {
                            source.reply(Component.text("  + $it"))
                        }
                        palette.size
                    }
                )
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
            client.brand = input.readString()
        }
        registerCustomPacketHandler("register") { _, client, input ->
            client.supportedChannels.addAll(
                input.readAvailable()
                    .toString(Charsets.US_ASCII)
                    .split(0.toChar())
                    .asSequence()
                    .map(Identifier::parse)
            )
        }
        registerCustomPacketHandler("unregister") { _, client, input ->
            client.supportedChannels.removeAll(
                input.readAvailable()
                    .toString(Charsets.US_ASCII)
                    .split(0.toChar())
                    .asSequence()
                    .map(Identifier::parse)
                    .toSet()
            )
        }
        registerCustomPacketHandler("worldedit:cui") { _, client, input ->
            LOGGER.info("CUI Packet from ${client.username}: ${input.readAvailable().toString(Charsets.US_ASCII)}")
        }
    }

    fun registerBlockHandler(handler: BlockHandler, vararg blocks: Identifier) {
        blocks.forEach { blockHandlers[it] = handler }
    }

    fun getBlockHandler(block: Identifier?) = blockHandlers[block] ?: DefaultBlockHandler

    private fun registerBlockHandlers() {
        DEFAULT_BLOCKSTATES.keys.forEach {
            val properties = BLOCK_STATE_PROPERTIES[it]!!
            if (properties.size == 1 && properties.keys.first() == "axis") {
                registerBlockHandler(PillarBlockHandler, it)
            } else if (it.value.endsWith("_sapling")) {
                registerBlockHandler(SaplingBlockHandler, it)
            } else if (it.value.endsWith("glass_pane")) {
                registerBlockHandler(PaneBlockHandler, it)
            }
        }
        registerBlockHandler(PaneBlockHandler, Identifier("iron_bars"))
    }

    fun registerItemHandler(handler: ItemHandler, vararg items: Identifier) {
        items.forEach { itemHandlers[it] = handler }
    }

    fun getItemHandler(item: Identifier?) = itemHandlers[item] ?: DefaultItemHandler

    private fun registerItemHandlers() {
        ITEM_ID_TO_PROTOCOL.keys.forEach { itemId ->
            if (itemId in DEFAULT_BLOCKSTATES) {
                registerItemHandler(BlockItemHandler(itemId), itemId)
            } else if (itemId.value.endsWith("_sword")) {
                registerItemHandler(SwordItemHandler, itemId)
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
                            val encoded = if (receiveChannel.availableForRead == 0) {
                                // Pre-1.4
                                "${config.motd.plainText()}\u00a7${clients.size}\u00a7${config.maxPlayers}"
                            } else {
                                // 1.4 through 1.6
                                "\u00a71\u0000127\u0000$MINECRAFT_VERSION" +
                                    "\u0000${LegacyComponentSerializer.legacySection().serialize(config.motd)}" +
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
                        packetInput.readString(255) // Server address
                        packetInput.readShort() // Server port
                        val nextStateInt = packetInput.readVarInt()
                        val nextState = try {
                            PacketState.values()[nextStateInt]
                        } catch (e: IndexOutOfBoundsException) {
                            LOGGER.warn("Unexpected packet state: ${nextStateInt.toString(16)}")
                            socket.dispose()
                            return@initialConnection
                        }
                        when (nextState) {
                            PacketState.STATUS -> handshakeJobs.add(launch {
                                try {
                                    StatusClient(this@MinecraftServer, socket, receiveChannel, sendChannel).handle()
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
                                    client.postHandshake()
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

    suspend fun broadcastExcept(client: PlayClient, packet: Packet) = broadcast(packet) { it !== client }
}

fun main(vararg args: String) {
    thread(name = "Timer-Hack-Thread", isDaemon = true) {
        while (true) {
            Thread.sleep(Long.MAX_VALUE)
        }
    }
    runBlocking { MinecraftServer(useJline = "--no-jline" !in args).run() }
}
