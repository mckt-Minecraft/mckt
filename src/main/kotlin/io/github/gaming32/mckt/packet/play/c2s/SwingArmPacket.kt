package io.github.gaming32.mckt.packet.play.c2s

import io.github.gaming32.mckt.packet.MinecraftInputStream
import io.github.gaming32.mckt.packet.MinecraftOutputStream
import io.github.gaming32.mckt.packet.Packet

class SwingArmPacket(val offhand: Boolean) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x2F
    }

    constructor(inp: MinecraftInputStream) : this(inp.readBoolean())

    override fun write(out: MinecraftOutputStream) = out.writeBoolean(offhand)
}
