package io.github.gaming32.mckt

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType.greedyString
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import com.mojang.brigadier.tree.CommandNode
import io.github.gaming32.mckt.commands.*
import io.github.gaming32.mckt.commands.arguments.getString
import io.github.gaming32.mckt.packet.*
import io.github.gaming32.mckt.packet.login.s2c.LoginDisconnectPacket
import io.github.gaming32.mckt.packet.play.PlayPingPacket
import io.github.gaming32.mckt.packet.play.s2c.*
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileNotFoundException
import kotlin.concurrent.thread
import kotlin.time.Duration.Companion.nanoseconds

private val LOGGER = getLogger()

class MinecraftServer {
    var running = true
    private val handshakeJobs = mutableSetOf<Job>()
    val clients = mutableMapOf<String, PlayClient>()
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
    internal var nextEntityId = 0

    private val consoleCommandSender = ConsoleCommandSource(this, "CONSOLE")
    val serverCommandSender = ConsoleCommandSource(this, "Server")
    internal val commandDispatcher = CommandDispatcher<CommandSource>()
    internal val helpTexts = mutableMapOf<CommandNode<CommandSource>, Component?>()

    @OptIn(DelicateCoroutinesApi::class)
    internal val threadPoolContext = if (Runtime.getRuntime().availableProcessors() == 1) {
        Dispatchers.Default
    } else {
        newFixedThreadPoolContext(Runtime.getRuntime().availableProcessors() - 1, "Heavy-Computation")
    }

    suspend fun run() = coroutineScope {
        LOGGER.info("Starting server...")

        BuiltinCommands.register()
        registerCommands()

        world = World(this@MinecraftServer, "world")
        world.findSpawnPoint().let { LOGGER.info("Found spawn point {}", it) }
        handleCommandsJob = launch { handleCommands() }
        acceptConnectionsJob = launch { acceptConnections() }
        LOGGER.info("Server started...")
        while (running) {
            val startTime = System.nanoTime()
            world.meta.time++
            if (world.meta.time % config.autosavePeriod == 0L) {
                launch { world.saveAndLog() }
                clients.values.forEach(PlayClient::save)
            }
            for (client in clients.values.toList()) {
                if (client.ended || client.receiveChannel.isClosedForRead || client.handlePacketsJob.isCompleted) {
                    client.handlePacketsJob.cancelAndJoin()
                    LOGGER.info("{} left the game.", client.username)
                    clients.remove(client.username)
                    client.close()
                    broadcast(PlayerListUpdatePacket(
                        PlayerListUpdatePacket.RemovePlayer(client.uuid)
                    ))
                    broadcast(RemoveEntitiesPacket(client.entityId))
                    continue
                }
                if (world.meta.time % 20 == 0L) {
                    client.sendPacket(UpdateTimePacket(world.meta.time))
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
                    tickTime / 1000.0, tickTime / 50.0
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
            client.sendPacket(PlayDisconnectPacket(
                Component.translatable("multiplayer.disconnect.server_shutdown")
            ))
            client.socket.dispose()
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
        LOGGER.info("Server stopped")
    }

    fun registerCommand(description: Component?, command: LiteralArgumentBuilder<CommandSource>) {
        val registered = commandDispatcher.register(command)
        helpTexts[registered] = description
    }

    private fun registerCommands() {
        registerCommand(Component.text("Send a message"), literal<CommandSource>("say")
            .then(argument<CommandSource, String>("message", greedyString())
                .executesSuspend {
                    val message = getString("message")
                    LOGGER.info("CHAT: [{}] {}", source.displayName.plainText(), message)
                    source.server.broadcast(SystemChatPacket(
                        Component.translatable("chat.type.announcement", source.displayName, Component.text(message))
                    ))
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
    }

    @Suppress("RedundantAsync")
    @OptIn(DelicateCoroutinesApi::class)
    private suspend fun handleCommands() {
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
                        val packetInput = MinecraftInputStream(ByteArrayInputStream(packet))
                        val protocolVersion = packetInput.readVarInt()
                        packetInput.readString(255) // Server address
                        @Suppress("BlockingMethodInNonBlockingContext")
                        packetInput.readUnsignedShort() // Server port
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
                                        oldClient.sendPacket(PlayDisconnectPacket(
                                            Component.translatable("multiplayer.disconnect.duplicate_login")
                                        ))
                                        oldClient.socket.dispose()
                                    }
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

fun main() {
    thread(name = "Timer-Hack-Thread", isDaemon = true) {
        while (true) {
            Thread.sleep(Long.MAX_VALUE)
        }
    }
    runBlocking { MinecraftServer().run() }
}
