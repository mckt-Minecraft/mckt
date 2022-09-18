package io.github.gaming32.mckt.packet.play.c2s

import io.github.gaming32.mckt.data.MinecraftOutputStream
import io.github.gaming32.mckt.data.readLong
import io.github.gaming32.mckt.data.readString
import io.github.gaming32.mckt.data.readVarInt
import io.github.gaming32.mckt.packet.Packet
import java.io.InputStream

data class CommandPacket(val command: String, val timestamp: Long) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x04
    }

    constructor(inp: InputStream) : this(inp.readString(), inp.readLong()) {
        inp.readLong() // Salt
        repeat(inp.readVarInt()) { // Signature[] length
            inp.readString() // Argument name
            inp.skip(inp.readVarInt().toLong()) // Signature
        }
    }

    override fun write(out: MinecraftOutputStream) {
        out.writeString(command)
        out.writeLong(timestamp)
        out.writeLong(0L) // Salt
        out.writeVarInt(0) // Signature[] length
        out.writeBoolean(false) // Signed preview
    }
}
