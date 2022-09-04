package io.github.gaming32.mckt.packet.play

import io.github.gaming32.mckt.packet.MinecraftInputStream
import io.github.gaming32.mckt.packet.MinecraftOutputStream
import io.github.gaming32.mckt.packet.Packet

class PlayPingPacket(val id: Int) : Packet(S2C_TYPE) {
    companion object {
        const val S2C_TYPE = 0x2F
        const val C2S_TYPE = 0x20
    }

    constructor(inp: MinecraftInputStream) : this(inp.readInt())

    override fun write(out: MinecraftOutputStream) = out.writeInt(id)
}
