package io.github.gaming32.mckt

import io.github.gaming32.mckt.packet.MinecraftInputStream
import io.github.gaming32.mckt.packet.PacketState
import io.github.gaming32.mckt.packet.readVarInt
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.*
import java.io.ByteArrayInputStream

private val LOGGER = getLogger()

class MinecraftServer {
    private var running = true
    private val statusJobs = mutableSetOf<Job>()
    private val clients = mutableSetOf<PlayClient>()
    private lateinit var handleCommandsJob: Job
    private lateinit var acceptConnectionsJob: Job

    suspend fun run() = coroutineScope {
        LOGGER.info("Starting server...")
        handleCommandsJob = launch { handleCommands() }
        acceptConnectionsJob = launch { acceptConnections() }
        while (running) {
            yield()
        }
        LOGGER.info("Stopping server...")
        acceptConnectionsJob.cancel()
        joinAll(handleCommandsJob, acceptConnectionsJob)
        statusJobs.forEach { it.cancel() }
        statusJobs.joinAll()
    }

    private suspend fun handleCommands() = coroutineScope {
        while (running) {
            val command = withContext(Dispatchers.IO) { readlnOrNull() }?.trim()?.ifEmpty { null } ?: continue
            when (command) {
                "help" -> for (line in """
                    List of commands:
                      + help -- Shows this help
                      + stop -- Stops the server
                """.trimIndent().lineSequence()) {
                    LOGGER.info(line)
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
                        PacketState.STATUS -> statusJobs.add(launch {
                            StatusClient(socket, receiveChannel, sendChannel).handle()
                            statusJobs.remove(coroutineContext[Job])
                        })
                        PacketState.LOGIN -> clients.add(PlayClient(socket, receiveChannel, sendChannel).apply {
                            handshake()
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
