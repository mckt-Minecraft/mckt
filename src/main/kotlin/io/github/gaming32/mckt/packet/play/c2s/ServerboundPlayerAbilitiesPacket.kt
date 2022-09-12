package io.github.gaming32.mckt.packet.play.c2s

import io.github.gaming32.mckt.packet.MinecraftInputStream
import io.github.gaming32.mckt.packet.MinecraftOutputStream
import io.github.gaming32.mckt.packet.Packet

data class ServerboundPlayerAbilitiesPacket(val flying: Boolean) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x1C
    }

    constructor(inp: MinecraftInputStream) : this((inp.readUnsignedByte() and 0x02) != 0)

    override fun write(out: MinecraftOutputStream) = out.writeByte(if (flying) 0x02 else 0x00)
}
