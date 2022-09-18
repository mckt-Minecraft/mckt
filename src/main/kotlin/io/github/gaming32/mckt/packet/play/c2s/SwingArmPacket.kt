package io.github.gaming32.mckt.packet.play.c2s

import io.github.gaming32.mckt.data.MinecraftOutputStream
import io.github.gaming32.mckt.data.readBoolean
import io.github.gaming32.mckt.packet.Packet
import java.io.InputStream

data class SwingArmPacket(val offhand: Boolean) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x2F
    }

    constructor(inp: InputStream) : this(inp.readBoolean())

    override fun write(out: MinecraftOutputStream) = out.writeBoolean(offhand)
}
