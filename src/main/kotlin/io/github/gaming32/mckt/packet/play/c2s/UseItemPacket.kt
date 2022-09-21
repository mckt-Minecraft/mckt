package io.github.gaming32.mckt.packet.play.c2s

import io.github.gaming32.mckt.data.readBoolean
import io.github.gaming32.mckt.data.readVarInt
import io.github.gaming32.mckt.data.writeBoolean
import io.github.gaming32.mckt.data.writeVarInt
import io.github.gaming32.mckt.packet.Packet
import java.io.InputStream
import java.io.OutputStream

data class UseItemPacket(val offhand: Boolean, val sequence: Int) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x32
    }

    constructor(inp: InputStream) : this(
        inp.readBoolean(),
        inp.readVarInt()
    )

    override fun write(out: OutputStream) {
        out.writeBoolean(offhand)
        out.writeVarInt(sequence)
    }
}
