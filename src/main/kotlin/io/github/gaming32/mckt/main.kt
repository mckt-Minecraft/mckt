package io.github.gaming32.mckt

import io.github.gaming32.mckt.packet.MinecraftInputStream
import io.github.gaming32.mckt.packet.PacketState
import io.github.gaming32.mckt.packet.play.s2c.PlayDisconnectPacket
import io.github.gaming32.mckt.packet.readVarInt
import io.github.gaming32.mckt.packet.writePacket
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import java.io.ByteArrayInputStream
import kotlin.time.Duration.Companion.nanoseconds

private val LOGGER = getLogger()

class MinecraftServer {
    private var running = true
    private val handshakeJobs = mutableSetOf<Job>()
    val clients = mutableMapOf<String, PlayClient>()
    private lateinit var handleCommandsJob: Job
    private lateinit var acceptConnectionsJob: Job

    suspend fun run() = coroutineScope {
        LOGGER.info("Starting server...")
        handleCommandsJob = launch { handleCommands() }
        acceptConnectionsJob = launch { acceptConnections() }
        while (running) {
            val startTime = System.nanoTime()
            // Use toList to capture a snapshot of the set, since we may modify it in this loop
            val clientsIterator = clients.values.iterator()
            while (clientsIterator.hasNext()) {
                val client = clientsIterator.next()
                if (client.receiveChannel.isClosedForRead) {
                    LOGGER.info("{} left the game.", client.username)
                    clientsIterator.remove()
                    continue
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
            client.sendChannel.writePacket(PlayDisconnectPacket(
                Component.text("Server closed")
            ))
            client.socket.dispose()
        }
        clients.clear()
        acceptConnectionsJob.cancel()
        handshakeJobs.forEach { it.cancel() }
        handshakeJobs.joinAll()
        handshakeJobs.clear()
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
                      + help -- Shows this help
                      + kick -- Kicks a player
                      + stop -- Stops the server
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
                        client.sendChannel.writePacket(PlayDisconnectPacket(reason))
                        client.socket.dispose()
                        LOGGER.info("Kicked {} for {}", username, reason.plainText())
                    }
                } catch (e: Exception) {
                    LOGGER.warn("{}", e.localizedMessage)
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
                    val packetLength = receiveChannel.readVarInt()
                    val bytesRead = receiveChannel.totalBytesRead
                    val packetId = receiveChannel.readVarInt()
                    val packetIdLength = (receiveChannel.totalBytesRead - bytesRead).toInt()
                    if (packetLength == 254 && packetId == 122) {
                        // Legacy ping packet
                        val message = "\u00a71\u0000127\u0000$MINECRAFT_VERSION\u0000$DEFAULT_MOTD\u00000\u00001"
                        val encoded = message.toByteArray(Charsets.UTF_16BE)
                        sendChannel.writeShort(encoded.size.toShort())
                        sendChannel.writeFully(encoded, 0, encoded.size)
                        sendChannel.flush()
                        socket.dispose()
                        return@initialConnection
                    }
                    if (packetId != 0x00) {
                        socket.dispose()
                        return@initialConnection
                    }
                    val packet = ByteArray(packetLength - packetIdLength)
                    receiveChannel.readFully(packet, 0, packet.size)
                    val packetInput = MinecraftInputStream(ByteArrayInputStream(packet))
                    val protocolVersion = packetInput.readVarInt()
                    if (protocolVersion != PROTOCOL_VERSION) {
                        LOGGER.warn("Unsupported protocol version $protocolVersion")
                        socket.dispose()
                        return@initialConnection
                    }
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
                            } finally {
                                handshakeJobs.remove(coroutineContext[Job])
                            }
                        })
                        PacketState.LOGIN -> handshakeJobs.add(launch {
                            val client = PlayClient(this@MinecraftServer, socket, receiveChannel, sendChannel)
                            try {
                                client.handshake()
                            } finally {
                                handshakeJobs.remove(coroutineContext[Job])
                            }
                            if (!client.receiveChannel.isClosedForRead) {
                                LOGGER.info("{} joined the game.", client.username)
                                clients.put(client.username, client)?.also { oldClient ->
                                    if (oldClient.receiveChannel.isClosedForRead) return@also
                                    LOGGER.info("Another client with that username was already online")
                                    oldClient.sendChannel.writePacket(PlayDisconnectPacket(
                                        Component.text("You logged in from another location")
                                    ))
                                    oldClient.socket.dispose()
                                }
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
}

fun main() = runBlocking { MinecraftServer().run() }
