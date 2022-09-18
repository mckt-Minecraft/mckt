package io.github.gaming32.mckt.packet.play.s2c

import io.github.gaming32.mckt.data.MinecraftOutputStream
import io.github.gaming32.mckt.packet.Packet

data class SetCenterChunkPacket(val centerX: Int, val centerZ: Int) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x4B
    }

    override fun write(out: MinecraftOutputStream) {
        out.writeVarInt(centerX)
        out.writeVarInt(centerZ)
    }
}
