package io.github.gaming32.mckt.packet.play.s2c

import io.github.gaming32.mckt.packet.MinecraftOutputStream
import io.github.gaming32.mckt.packet.Packet
import java.util.UUID

class SpawnPlayerPacket(
    val entityId: Int,
    val uuid: UUID,
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float,
    val pitch: Float
) : Packet(TYPE) {
    companion object {
        val TYPE = 0x02
    }

    override fun write(out: MinecraftOutputStream) {
        out.writeVarInt(entityId)
        out.writeUuid(uuid)
        out.writeDouble(x)
        out.writeDouble(y)
        out.writeDouble(z)
        out.writeDegrees(yaw)
        out.writeDegrees(pitch)
    }
}
