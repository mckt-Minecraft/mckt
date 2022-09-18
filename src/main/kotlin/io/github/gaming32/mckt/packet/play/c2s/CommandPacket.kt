package io.github.gaming32.mckt.packet.play.c2s

import io.github.gaming32.mckt.data.*
import io.github.gaming32.mckt.packet.Packet
import java.io.InputStream
import java.io.OutputStream

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

    override fun write(out: OutputStream) {
        out.writeString(command)
        out.writeLong(timestamp)
        out.writeLong(0L) // Salt
        out.writeVarInt(0) // Signature[] length
        out.writeBoolean(false) // Signed preview
    }
}
