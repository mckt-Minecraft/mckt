package io.github.gaming32.mckt.packet.play.s2c

import io.github.gaming32.mckt.packet.MinecraftOutputStream
import io.github.gaming32.mckt.packet.Packet

class PlayerPositionSyncPacket(
    val teleportId: Int,
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float,
    val pitch: Float,
    val xIsRelative: Boolean = false,
    val yIsRelative: Boolean = false,
    val zIsRelative: Boolean = false,
    val yawIsRelative: Boolean = false,
    val pitchIsRelative: Boolean = false,
    val dismountVehicle: Boolean = true
) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x39
    }

    override fun write(out: MinecraftOutputStream) {
        out.writeDouble(x)
        out.writeDouble(y)
        out.writeDouble(z)
        out.writeFloat(yaw)
        out.writeFloat(pitch)
        out.writeByte(
            (if (xIsRelative) 0x01 else 0) or
            (if (yIsRelative) 0x02 else 0) or
            (if (zIsRelative) 0x04 else 0) or
            (if (yawIsRelative) 0x08 else 0) or
            (if (pitchIsRelative) 0x10 else 0)
        )
        out.writeVarInt(teleportId)
        out.writeBoolean(dismountVehicle)
    }
}
