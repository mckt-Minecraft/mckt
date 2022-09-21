package io.github.gaming32.mckt.packet.play.s2c

import io.github.gaming32.mckt.data.writeShort
import io.github.gaming32.mckt.data.writeVarInt
import io.github.gaming32.mckt.packet.Packet
import java.io.OutputStream
import kotlin.math.roundToInt

data class SetEntityVelocityPacket(
    val entityId: Int,
    val velocityX: Double,
    val velocityY: Double,
    val velocityZ: Double
) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x52
    }

    override fun write(out: OutputStream) {
        out.writeVarInt(entityId)
        out.writeShort((velocityX * 8000).roundToInt())
        out.writeShort((velocityY * 8000).roundToInt())
        out.writeShort((velocityZ * 8000).roundToInt())
    }
}
