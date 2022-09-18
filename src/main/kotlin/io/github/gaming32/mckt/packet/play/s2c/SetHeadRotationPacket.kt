package io.github.gaming32.mckt.packet.play.s2c

import io.github.gaming32.mckt.data.MinecraftOutputStream
import io.github.gaming32.mckt.packet.Packet

data class SetHeadRotationPacket(val entityId: Int, val headYaw: Float) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x3F
    }

    override fun write(out: MinecraftOutputStream) {
        out.writeVarInt(entityId)
        out.writeDegrees(headYaw)
    }
}
