package io.github.gaming32.mckt.packet.play.s2c

import io.github.gaming32.mckt.data.writeShort
import io.github.gaming32.mckt.data.writeVarInt
import io.github.gaming32.mckt.objects.Vector3d
import io.github.gaming32.mckt.packet.Packet
import java.io.OutputStream
import kotlin.math.roundToInt

data class SetEntityVelocityPacket(
    val entityId: Int,
    val velocity: Vector3d
) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x52
    }

    override fun write(out: OutputStream) {
        out.writeVarInt(entityId)
        out.writeShort((velocity.x * 8000).roundToInt())
        out.writeShort((velocity.y * 8000).roundToInt())
        out.writeShort((velocity.z * 8000).roundToInt())
    }
}
