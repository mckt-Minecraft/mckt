package io.github.gaming32.mckt.packet

import io.ktor.utils.io.*
import java.io.ByteArrayOutputStream

abstract class Packet(val type: Int) {
    abstract fun write(out: MinecraftOutputStream)

    suspend fun writePacket(channel: ByteWriteChannel) {
        val output = ByteArrayOutputStream()
        val mcOut = MinecraftOutputStream(output)
        mcOut.writeVarInt(type)
        write(mcOut)
        channel.writeVarInt(output.size())
        channel.writeFully(output.toByteArray())
        channel.flush()
    }
}
