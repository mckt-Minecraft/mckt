package io.github.gaming32.mckt.packet.play.c2s

import io.github.gaming32.mckt.data.readUShort
import io.github.gaming32.mckt.data.writeShort
import io.github.gaming32.mckt.packet.Packet
import java.io.InputStream
import java.io.OutputStream

data class ServerboundSetHeldItemPacket(val slot: Int) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x28
    }

    constructor(inp: InputStream) : this(inp.readUShort().toInt())

    override fun write(out: OutputStream) = out.writeShort(slot)
}
