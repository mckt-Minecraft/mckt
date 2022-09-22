package io.github.gaming32.mckt

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType.integer
import com.mojang.brigadier.arguments.StringArgumentType.greedyString
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.CommandNode
import com.mojang.brigadier.tree.LiteralCommandNode
import io.github.gaming32.mckt.GlobalPalette.DEFAULT_BLOCKSTATES
import io.github.gaming32.mckt.blocks.BlockHandler
import io.github.gaming32.mckt.blocks.DefaultBlockHandler
import io.github.gaming32.mckt.commands.*
import io.github.gaming32.mckt.commands.arguments.*
import io.github.gaming32.mckt.commands.arguments.TextArgumentType.getTextComponent
import io.github.gaming32.mckt.data.readShort
import io.github.gaming32.mckt.data.readString
import io.github.gaming32.mckt.data.readVarInt
import io.github.gaming32.mckt.items.*
import io.github.gaming32.mckt.objects.BlockPosition
import io.github.gaming32.mckt.objects.BlockState
import io.github.gaming32.mckt.objects.Identifier
import io.github.gaming32.mckt.objects.Vector3d
import io.github.gaming32.mckt.packet.Packet
import io.github.gaming32.mckt.packet.PacketState
import io.github.gaming32.mckt.packet.login.s2c.LoginDisconnectPacket
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
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.jline.reader.*
import org.jline.utils.AttributedString
import org.jline.utils.AttributedStringBuilder
import org.jline.utils.AttributedStyle
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileNotFoundException
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlin.concurrent.thread
import kotlin.io.path.deleteIfExists
import kotlin.io.path.forEachDirectoryEntry
import kotlin.math.min
import kotlin.time.Duration.Companion.nanoseconds

private val LOGGER = getLogger()
private val STYLES = listOf(
    AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN),
    AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW),
    AttributedStyle.DEFAULT.foreground(AttributedStyle.MAGENTA)
)

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
    internal val commandDispatcher = CommandDispatcher<CommandSource>()
    internal val helpTexts = mutableMapOf<CommandNode<CommandSource>, Component?>()

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
    }

    suspend fun run() = coroutineScope {
        LOGGER.info("Starting server...")

        registerCommands()
        registerBlockHandlers()
        registerItemHandlers()

        world = World(this@MinecraftServer, "world")

        LOGGER.info("Searching for spawn point...")
        world.findSpawnPoint().let { LOGGER.info("Found spawn point {}", it) }

        handleCommandsJob = launch { handleCommands() }
        acceptConnectionsJob = launch { acceptConnections() }
        yield()
        LOGGER.info("Server started")
        while (running) {
            val startTime = System.nanoTime()
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
            val endTime = System.nanoTime()
            val tickTime = (endTime - startTime).nanoseconds.inWholeMilliseconds
            if (tickTime >= 1000) {
                LOGGER.warn(
                    "Is the server overloaded? Running {} seconds ({} ticks) behind.",
                    (tickTime - 50) / 1000.0, (tickTime - 50) / 50.0
                )
            }
            val sleepTime = 50 - tickTime
            if (sleepTime <= 0) {
                yield()
            } else {
                delay(sleepTime)
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
        registerCommand(Component.text("Show this help"), literal<CommandSource>("help")
            .executesSuspend {
                source.reply(Component.text("Here's a list of the commands you can use:\n")
                    .append(Component.join(
                        JoinConfiguration.newlines(),
                        helpTexts.asSequence()
                            .filter { it.key.canUse(source) }
                            .map { (command, description) -> Component.text { builder ->
                                builder.append(Component.text("  + /${command.usageText} -- "))
                                if (description != null) {
                                    builder.append(description)
                                }
                            } }
                            .toList()
                    ))
                )
                0
            }
            .then(argument<CommandSource, String>("command", greedyString())
                .executesSuspend {
                    val commandName = getString("command")
                    var result = 0
                    source.reply(if (commandName == "all") {
                        Component.join(
                            JoinConfiguration.newlines(),
                            commandDispatcher.root.children
                                .asSequence()
                                .filter { it.canUse(source) }
                                .flatMap { command ->
                                    commandDispatcher.getAllUsage(command, source, true)
                                        .map {
                                            if (it.startsWith("${command.usageText} ->")) {
                                                "/$it" // Command alias
                                            } else {
                                                "/${command.usageText} $it"
                                            }
                                        }
                                }
                                .map(Component::text)
                                .toList()
                        )
                    } else {
                        val command = commandDispatcher.root.getChild(commandName)
                        if (command == null || !command.canUse(source)) {
                            result = 1
                            Component.translatable(
                                "commands.help.failed",
                                NamedTextColor.RED,
                                Component.text(commandName)
                            )
                        } else {
                            var commandForUsage = command
                            while (commandForUsage.redirect != null) {
                                commandForUsage = commandForUsage.redirect
                            }
                            Component.text { builder ->
                                builder.append(Component.join(
                                    JoinConfiguration.newlines(),
                                    commandDispatcher.getAllUsage(commandForUsage, source, true)
                                        .map { Component.text("/${command.usageText} $it") }
                                ))
                                source.server.helpTexts[command]?.let { description ->
                                    builder.append(Component.newline()).append(description)
                                }
                            }
                        }
                    })
                    result
                }
                .suggests { ctx, builder ->
                    if ("all".startsWith(builder.remainingLowerCase)) {
                        builder.suggest("all")
                    }
                    helpTexts.keys.forEach { node ->
                        if (
                            node.canUse(ctx.source) &&
                            node.usageText.startsWith(builder.remainingLowerCase, ignoreCase = true)
                        ) {
                            builder.suggest(node.usageText)
                        }
                    }
                    builder.buildFuture()
                }
            )
        )
        registerCommand(Component.text("Send a message"), literal<CommandSource>("say")
            .then(argument<CommandSource, String>("message", greedyString())
                .executesSuspend {
                    val message = getString("message")
                    source.server.broadcastChat(
                        Component.translatable("chat.type.announcement", source.displayName, Component.text(message))
                    )
                    0
                }
            )
        )
        registerCommand(Component.text("List the players online"), literal<CommandSource>("list")
            .executesSuspend {
                source.reply(Component.translatable(
                    "commands.list.players",
                    Component.text(clients.size),
                    Component.text(config.maxPlayers),
                    Component.join(
                        JoinConfiguration.commas(true),
                        clients.keys.map(Component::text)
                    )
                ))
                0
            }
        )
        registerCommand(Component.text("Gets a block at a position"), literal<CommandSource>("getblock")
            .requires { it.hasPermission(1) }
            .then(argument<CommandSource, PositionArgument>("position", BlockPositionArgumentType)
                .executesSuspend {
                    val position = getLoadedBlockPosition("position")
                    val block = world.getBlock(position)!!
                    source.reply(
                        Component.text("The block at ${position.x} ${position.y} ${position.z} is ")
                            .append(Component.text(block.toString(), NamedTextColor.GREEN))
                    )
                    0
                }
                .then(literal<CommandSource>("generate")
                    .executesSuspend {
                        val position = getBlockPosition("position")
                        val block = world.getBlockOrGenerate(position)
                        source.reply(
                            Component.text("The block at ${position.x} ${position.y} ${position.z} is ")
                                .append(Component.text(block.toString(), NamedTextColor.GREEN))
                        )
                        0
                    }
                )
            )
        )
        registerCommand(Component.text("Teleport a player"), literal<CommandSource>("tp").also { command ->
            command.requires { it.hasPermission(1) }
            suspend fun CommandContext<CommandSource>.teleport(
                entities: List<PlayClient>, destination: PlayClient
            ) {
                entities.forEach { it.teleport(destination) }
                source.replyBroadcast(
                    if (entities.size == 1) {
                        Component.translatable(
                            "commands.teleport.success.entity.single",
                            Component.text(entities[0].username),
                            Component.text(destination.username)
                        )
                    } else {
                        Component.translatable(
                            "commands.teleport.success.entity.multiple",
                            Component.text(entities.size),
                            Component.text(destination.username)
                        )
                    }
                )
            }
            suspend fun CommandContext<CommandSource>.teleport(
                entities: List<PlayClient>, destination: Vector3d
            ) {
                entities.forEach { it.teleport(destination.x, destination.y, destination.z) }
                source.replyBroadcast(
                    if (entities.size == 1) {
                        Component.translatable(
                            "commands.teleport.success.location.single",
                            Component.text(entities[0].username),
                            Component.text(destination.x),
                            Component.text(destination.y),
                            Component.text(destination.z)
                        )
                    } else {
                        Component.translatable(
                            "commands.teleport.success.location.multiple",
                            Component.text(entities.size),
                            Component.text(destination.x),
                            Component.text(destination.y),
                            Component.text(destination.z)
                        )
                    }
                )
            }
            command.then(argument<CommandSource, EntitySelector>("destination", entity())
                .executesSuspend {
                    teleport(listOf(source.entity), getEntity("destination"))
                    0
                }
            )
            command.then(argument<CommandSource, PositionArgument>("location", Vector3ArgumentType())
                .executesSuspend {
                    teleport(listOf(source.entity), getVec3("location"))
                    0
                }
            )
            command.then(argument<CommandSource, EntitySelector>("target", entities())
                .then(argument<CommandSource, EntitySelector>("destination", entity())
                    .executesSuspend {
                        teleport(getEntities("target"), getEntity("destination"))
                        0
                    }
                )
                .then(argument<CommandSource, PositionArgument>("location", Vector3ArgumentType())
                    .executesSuspend {
                        teleport(getEntities("target"), getVec3("location"))
                        0
                    }
                )
            )
        })
        registerCommandAliases("tp", "teleport")
        registerCommand(Component.text("Set player gamemode"), literal<CommandSource>("gamemode").also { command ->
            command.requires { it.hasPermission(1) }
            Gamemode.values().forEach { gamemode ->
                val gamemodeText = Component.translatable("gameMode.${gamemode.name.lowercase()}")
                command.then(literal<CommandSource>(gamemode.name.lowercase())
                    .executesSuspend {
                        source.player.setGamemode(gamemode)
                        source.replyBroadcast(Component.translatable("commands.gamemode.success.self", gamemodeText))
                        0
                    }
                    .then(argument<CommandSource, EntitySelector>("player", players())
                        .executesSuspend {
                            getPlayers("player").forEach { player ->
                                player.setGamemode(gamemode)
                                source.replyBroadcast(Component.translatable(
                                    "commands.gamemode.success.other",
                                    Component.text(player.username),
                                    gamemodeText
                                ))
                            }
                            0
                        }
                    )
                )
            }
        })
        registerCommand(Component.text("Send a custom message"), literal<CommandSource>("tellraw")
            .requires { it.hasPermission(1) }
            .then(argument<CommandSource, EntitySelector>("targets", players())
                .then(argument<CommandSource, Component>("message", TextArgumentType)
                    .executesSuspend {
                        val targets = getPlayers("targets")
                        val message = getTextComponent("message")
                        targets.forEach { it.sendMessage(message) }
                        if (source !is ClientCommandSource) {
                            source.reply(Component.text()
                                .append(Component.text("Sent raw message \""))
                                .append(message)
                                .append(Component.text("\" to ${targets.size} player(s)"))
                                .build()
                            )
                        }
                        0
                    }
                )
            )
        )
        registerCommand(Component.text("Forcefully disconnect a player"), literal<CommandSource>("kick")
            .requires { it.hasPermission(2) }
            .then(argument<CommandSource, EntitySelector>("player", players())
                .executesSuspend {
                    val reason = Component.translatable("multiplayer.disconnect.kicked")
                    getPlayers("player").forEach { player ->
                        player.kick(reason)
                        source.replyBroadcast(Component.translatable(
                            "commands.kick.success",
                            Component.text(player.username),
                            reason
                        ))
                    }
                    0
                }
                .then(argument<CommandSource, Component>("reason", TextArgumentType)
                    .executesSuspend {
                        val reason = getTextComponent("reason")
                        getPlayers("player").forEach { player ->
                            player.kick(reason)
                            source.replyBroadcast(Component.translatable(
                                "commands.kick.success",
                                Component.text(player.username),
                                reason
                            ))
                        }
                        0
                    }
                )
            )
        )
        registerCommand(Component.text("Sets a player's operator level"), literal<CommandSource>("op")
            .requires { it.hasPermission(3) }
            .then(argument<CommandSource, EntitySelector>("player", players())
                .executesSuspend {
                    val level = min(2, source.operator)
                    getPlayers("player").forEach { player ->
                        player.setOperatorLevel(level)
                        source.replyBroadcast(Component.translatable(
                            "commands.op.success",
                            Component.text(player.username),
                            Component.text(level)
                        ))
                    }
                    0
                }
                .then(argument<CommandSource, Int>("level", integer(0))
                    .executesSuspend {
                        val level = getInteger("level")
                        if (level > source.operator) {
                            source.reply(Component.text(
                                "Cannot give player a higher operator level than you",
                                NamedTextColor.RED
                            ))
                            return@executesSuspend 1
                        }
                        getPlayers("player").forEach { player ->
                            player.setOperatorLevel(level)
                            source.replyBroadcast(Component.translatable(
                                "commands.op.success",
                                Component.text(player.username),
                                Component.text(level)
                            ))
                        }
                        0
                    }
                )
            )
        )
        registerCommand(Component.text("Sets a player's operator level to 0"), literal<CommandSource>("deop")
            .requires { it.hasPermission(3) }
            .then(argument<CommandSource, EntitySelector>("player", players())
                .executesSuspend {
                    getPlayers("player").forEach { player ->
                        player.setOperatorLevel(0)
                        source.replyBroadcast(Component.translatable(
                            "commands.deop.success",
                            Component.text(player.username)
                        ))
                    }
                    0
                }
            )
        )
        registerCommand(Component.text("Stops the server"), literal<CommandSource>("stop")
            .requires { it.hasPermission(4) }
            .executesSuspend {
                source.replyBroadcast(Component.translatable("commands.stop.stopping"))
                running = false
                0
            }
        )
        registerCommand(Component.text("Saves the world"), literal<CommandSource>("save")
            .requires { it.hasPermission(4) }
            .executesSuspend {
                world.saveAndLog(source)
                0
            }
        )
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
        )
    }

    fun registerBlockHandler(handler: BlockHandler, vararg blocks: Identifier) {
        blocks.forEach { blockHandlers[it] = handler }
    }

    fun getBlockHandler(block: Identifier?) = blockHandlers[block] ?: DefaultBlockHandler

    private fun registerBlockHandlers() = Unit

    fun registerItemHandler(handler: ItemHandler, vararg items: Identifier) {
        items.forEach { itemHandlers[it] = handler }
    }

    fun getItemHandler(item: Identifier?) = itemHandlers[item] ?: DefaultItemHandler

    private fun registerItemHandlers() {
        registerItemHandler(
            SimpleBlockItemHandler,
            *DEFAULT_BLOCKSTATES.keys.filter(ITEM_ID_TO_PROTOCOL::containsKey).toTypedArray()
        )
        registerItemHandler(
            LogItemHandler,
            *DEFAULT_BLOCKSTATES.keys
                .filter { it.value.endsWith("_log") } // It works, I guess /shrug
                .toTypedArray()
        )
        registerItemHandler(DebugStickItemHandler, Identifier("debug_stick"))
        registerItemHandler(FireworkRocketHandler, Identifier("firework_rocket"))
        registerItemHandler(FlintAndSteelHandler, Identifier("flint_and_steel"))
    }

    fun getPlayerByName(name: String) = clients[name]

    fun getPlayerByUuid(uuid: UUID) = clients.values.firstOrNull { it.uuid == uuid }

    inline fun getPlayers(predicate: (PlayClient) -> Boolean) = clients.values.filter(predicate)

    suspend fun setBlock(location: BlockPosition, block: BlockState) {
        world.setBlock(location, block)
        broadcast(SetBlockPacket(location, block))
    }

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
