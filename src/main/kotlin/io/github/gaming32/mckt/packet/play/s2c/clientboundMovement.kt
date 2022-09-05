package io.github.gaming32.mckt.packet.play.s2c

import io.github.gaming32.mckt.packet.MinecraftOutputStream
import io.github.gaming32.mckt.packet.Packet

class EntityTeleportPacket(
    val entityId: Int,
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float,
    val pitch: Float,
    val onGround: Boolean
) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x66
    }

    override fun write(out: MinecraftOutputStream) {
        out.writeVarInt(entityId)
        out.writeDouble(x)
        out.writeDouble(y)
        out.writeDouble(z)
        out.writeDegrees(yaw.toDouble())
        out.writeDegrees(pitch.toDouble())
        out.writeBoolean(onGround)
    }
}
