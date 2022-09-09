package io.github.gaming32.mckt.packet

import io.github.gaming32.mckt.getLogger
import io.ktor.utils.io.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*
import java.util.zip.Deflater

private val writeLocks = WeakHashMap<ByteWriteChannel, Mutex>()
private val DEFLATER = Deflater()
private val BUFFER = ByteArray(8192)
private val LOGGER = getLogger()

abstract class Packet(val type: Int) {
    abstract fun write(out: MinecraftOutputStream)

    suspend fun writePacket(channel: ByteWriteChannel, compression: Int) {
        var output = ByteArrayOutputStream()
        MinecraftOutputStream(output).let { mcOut ->
            mcOut.writeVarInt(type)
            write(mcOut)
        }
        if (compression != -1) {
            val out2 = ByteArrayOutputStream()
            MinecraftOutputStream(out2).let { mcOut ->
                if (output.size() > compression) {
                    mcOut.writeVarInt(output.size())
                    DEFLATER.setInput(output.toByteArray())
                    DEFLATER.finish()
                    while (!DEFLATER.finished()) {
                        out2.write(BUFFER, 0, DEFLATER.deflate(BUFFER))
                    }
                    DEFLATER.reset()
                } else {
                    mcOut.writeVarInt(0)
                    mcOut.write(output.toByteArray())
                }
            }
            output = out2
        }
        try {
            writeLocks.computeIfAbsent(channel) { Mutex() }.withLock {
                channel.writeFully(encodeData {
                    writeVarInt(output.size())
                    write(output.toByteArray())
                })
            }
            channel.flush()
        } catch (e: IOException) {
            LOGGER.error("Client connection had error", e)
            throw e
        }
    }
}

suspend fun ByteWriteChannel.sendPacket(packet: Packet, compression: Int) = packet.writePacket(this, compression)
