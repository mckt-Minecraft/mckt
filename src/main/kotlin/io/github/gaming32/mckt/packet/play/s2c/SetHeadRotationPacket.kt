package io.github.gaming32.mckt.packet.play.s2c

import io.github.gaming32.mckt.data.writeDegrees
import io.github.gaming32.mckt.data.writeVarInt
import io.github.gaming32.mckt.packet.Packet
import java.io.OutputStream

data class SetHeadRotationPacket(val entityId: Int, val headYaw: Float) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x3F
    }

    override fun write(out: OutputStream) {
        out.writeVarInt(entityId)
        out.writeDegrees(headYaw)
    }
}
