package io.github.gaming32.mckt.packet.play.s2c

import io.github.gaming32.mckt.packet.MinecraftOutputStream
import io.github.gaming32.mckt.packet.Packet

class EntityAnimationPacket(val entityId: Int, val animation: UByte) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x03
        const val SWING_MAINHAND: UByte = 0u
        const val TAKE_DAMAGE: UByte = 1u
        const val LEAVE_BED: UByte = 2u
        const val SWING_OFFHAND: UByte = 3u
        const val CRIT_EFFECT: UByte = 4u
        const val ENCHANTED_CRIT_EFFECT: UByte = 5u
    }

    override fun write(out: MinecraftOutputStream) {
        out.writeVarInt(entityId)
        out.writeByte(animation.toInt())
    }
}
