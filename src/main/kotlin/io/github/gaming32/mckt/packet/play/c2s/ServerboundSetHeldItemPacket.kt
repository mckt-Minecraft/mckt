package io.github.gaming32.mckt.packet.play.c2s

import io.github.gaming32.mckt.data.MinecraftOutputStream
import io.github.gaming32.mckt.data.readUShort
import io.github.gaming32.mckt.packet.Packet
import java.io.InputStream

data class ServerboundSetHeldItemPacket(val slot: Int) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x28
    }

    constructor(inp: InputStream) : this(inp.readUShort().toInt())

    override fun write(out: MinecraftOutputStream) = out.writeShort(slot)
}
