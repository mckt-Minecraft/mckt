package io.github.gaming32.mckt.packet.play.s2c

import io.github.gaming32.mckt.data.writeDegrees
import io.github.gaming32.mckt.data.writeDouble
import io.github.gaming32.mckt.data.writeUuid
import io.github.gaming32.mckt.data.writeVarInt
import io.github.gaming32.mckt.packet.Packet
import java.io.OutputStream
import java.util.*

data class SpawnPlayerPacket(
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

    override fun write(out: OutputStream) {
        out.writeVarInt(entityId)
        out.writeUuid(uuid)
        out.writeDouble(x)
        out.writeDouble(y)
        out.writeDouble(z)
        out.writeDegrees(yaw)
        out.writeDegrees(pitch)
    }
}
