package io.github.gaming32.mckt.packet

import io.ktor.utils.io.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.ByteArrayOutputStream
import java.util.*

private val writeLocks = WeakHashMap<ByteWriteChannel, Mutex>()

abstract class Packet(val type: Int) {
    abstract fun write(out: MinecraftOutputStream)

    suspend fun writePacket(channel: ByteWriteChannel) {
        val output = ByteArrayOutputStream()
        val mcOut = MinecraftOutputStream(output)
        mcOut.writeVarInt(type)
        write(mcOut)
        writeLocks.computeIfAbsent(channel) { Mutex() }.withLock {
            channel.writeFully(encodeData {
                writeVarInt(output.size())
                write(output.toByteArray())
            })
        }
        channel.flush()
    }
}

suspend fun ByteWriteChannel.sendPacket(packet: Packet) = packet.writePacket(this)
