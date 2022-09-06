package io.github.gaming32.mckt

import io.github.gaming32.mckt.packet.*
import io.github.gaming32.mckt.packet.login.s2c.LoginDisconnectPacket
import io.github.gaming32.mckt.packet.play.PlayPingPacket
import io.github.gaming32.mckt.packet.play.s2c.PlayDisconnectPacket
import io.github.gaming32.mckt.packet.play.s2c.PlayerListUpdatePacket
import io.github.gaming32.mckt.packet.play.s2c.UpdateTimePacket
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileNotFoundException
import java.lang.NumberFormatException
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

    suspend fun run() = coroutineScope {
        LOGGER.info("Starting server...")
        world = World(this@MinecraftServer, "world")
        handleCommandsJob = launch { handleCommands() }
        acceptConnectionsJob = launch { acceptConnections() }
        while (running) {
            val startTime = System.nanoTime()
            world.meta.time++
            if (world.meta.time % 6000 == 0L) {
                world.save()
                clients.values.forEach(PlayClient::save)
            }
            val clientsIterator = clients.values.iterator()
            while (clientsIterator.hasNext()) {
                val client = clientsIterator.next()
                if (client.receiveChannel.isClosedForRead || client.handlePacketsJob.isCompleted) {
                    client.handlePacketsJob.cancelAndJoin()
                    LOGGER.info("{} left the game.", client.username)
                    clientsIterator.remove()
                    client.close()
                    broadcast(PlayerListUpdatePacket(
                        PlayerListUpdatePacket.RemovePlayer(client.uuid)
                    ))
                    continue
                }
                if (world.meta.time % 20 == 0L) {
                    client.sendChannel.sendPacket(UpdateTimePacket(world.meta.time))
                    if (client.pingId == -1) {
                        client.pingId = client.nextPingId++
                        client.sendChannel.sendPacket(PlayPingPacket(client.pingId))
                        client.pingStart = System.nanoTime()
                    }
                }
            }
            val endTime = System.nanoTime()
            val sleepTime = 50 - (endTime - startTime).nanoseconds.inWholeMilliseconds
            if (sleepTime <= 0) {
                yield()
            } else {
                delay(sleepTime)
            }
        }
        LOGGER.info("Stopping server...")
        for (client in clients.values) {
            client.sendChannel.sendPacket(PlayDisconnectPacket(
                Component.text("Server closed")
            ))
            client.socket.dispose()
            client.close()
        }
        clients.clear()
        acceptConnectionsJob.cancel()
        handshakeJobs.forEach { it.cancel() }
        handshakeJobs.joinAll()
        handshakeJobs.clear()
        world.close()
        joinAll(handleCommandsJob, acceptConnectionsJob)
    }

    private suspend fun handleCommands() = coroutineScope {
        while (running) {
            val command = withContext(Dispatchers.IO) { readlnOrNull() }?.trim()?.ifEmpty { null } ?: continue
            val (baseCommand, rest) = if (' ' in command) {
                command.split(' ', limit = 2)
            } else {
                listOf(command, "")
            }
            when (baseCommand) {
                "help" -> for (line in """
                    List of commands:
                      + help     -- Shows this help
                      + kick     -- Kicks a player
                      + save     -- Saves the world
                      + getblock -- Gets a block
                      + stop     -- Stops the server
                """.trimIndent().lineSequence()) {
                    LOGGER.info(line)
                }
                "kick" -> try {
                    val spaceIndex = rest.indexOf(' ')
                    val (username, reason) = if (spaceIndex != -1) {
                        Pair(
                            rest.substring(0, spaceIndex),
                            GsonComponentSerializer.gson().deserialize(rest.substring(spaceIndex + 1))
                        )
                    } else {
                        Pair(rest, Component.text("Kicked by operator"))
                    }
                    val client = clients[username]
                    if (client == null || client.receiveChannel.isClosedForRead) {
                        LOGGER.warn("Player {} is not online.", username)
                    } else {
                        client.sendChannel.sendPacket(PlayDisconnectPacket(reason))
                        client.socket.dispose()
                        LOGGER.info("Kicked {} for {}", username, reason.plainText())
                    }
                } catch (e: Exception) {
                    LOGGER.warn("{}", e.localizedMessage)
                }
                "save" -> {
                    world.save()
                    clients.values.forEach(PlayClient::save)
                    LOGGER.info("Saved world")
                }
                "getblock" -> {
                    if (rest.count(' '::equals) != 2) {
                        LOGGER.warn("Syntax: getblock <x> <y> <z>")
                        continue
                    }
                    val (x, y, z) = try {
                        rest.split(' ').map(String::toInt)
                    } catch (e: NumberFormatException) {
                        LOGGER.warn("Syntax: getblock <x> <y> <z>")
                        continue
                    }
                    LOGGER.info("The block at {} {} {} is {}", x, y, z, world.getBlock(x, y, z))
                }
                "stop" -> running = false
                else -> LOGGER.warn("Unknown command: {}", command)
            }
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
                    val packetLength = receiveChannel.readVarInt(specialFe = true)
                    if (packetLength == 0xFE) {
                        val encoded = if (receiveChannel.availableForRead == 0) {
                            // Pre-1.4
                            PlainTextComponentSerializer.plainText().serialize(config.motd) +
                                "\u00a7${clients.size}\u00a7${config.maxPlayers}"
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
                                sendChannel.sendPacket(LoginDisconnectPacket(Component.text(message)))
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
                                    oldClient.sendChannel.sendPacket(PlayDisconnectPacket(
                                        Component.text("You logged in from another location")
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
                }
            }
        }
    }

    suspend fun broadcast(packet: Packet) = clients.values.forEach { it.sendChannel.sendPacket(packet) }

    internal suspend inline fun broadcastIf(packet: Packet, condition: (PlayClient) -> Boolean) =
        clients.values.forEach { client ->
            if (!condition(client)) return@forEach
            client.sendChannel.sendPacket(packet)
        }

    suspend fun broadcast(packet: Packet, condition: (PlayClient) -> Boolean) = broadcastIf(packet, condition)

    suspend fun broadcastExcept(client: PlayClient, packet: Packet) = broadcastIf(packet) { it !== client }
}

fun main() {
    thread(isDaemon = true) {
        while (true) {
            Thread.sleep(Long.MAX_VALUE)
        }
    }
    runBlocking { MinecraftServer().run() }
}
