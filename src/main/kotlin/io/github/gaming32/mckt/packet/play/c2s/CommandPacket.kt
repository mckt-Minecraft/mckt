package io.github.gaming32.mckt.packet.play.c2s

import io.github.gaming32.mckt.packet.MinecraftInputStream
import io.github.gaming32.mckt.packet.MinecraftOutputStream
import io.github.gaming32.mckt.packet.Packet
import io.github.gaming32.mckt.packet.readVarInt

class CommandPacket(val command: String, val timestamp: Long) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x04
    }

    constructor(inp: MinecraftInputStream) : this(inp.readString(), inp.readLong()) {
        inp.readLong() // Salt
        repeat(inp.readVarInt()) { // Signature[] length
            inp.readString() // Argument name
            inp.skipBytes(inp.readVarInt()) // Signature
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
