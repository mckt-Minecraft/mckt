package io.github.gaming32.mckt.packet.play.c2s

import io.github.gaming32.mckt.packet.MinecraftInputStream
import io.github.gaming32.mckt.packet.MinecraftOutputStream
import io.github.gaming32.mckt.packet.Packet

data class ServerboundSetHeldItemPacket(val slot: Int) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x28
    }

    constructor(inp: MinecraftInputStream) : this(inp.readUnsignedShort())

    override fun write(out: MinecraftOutputStream) = out.writeShort(slot)
}
