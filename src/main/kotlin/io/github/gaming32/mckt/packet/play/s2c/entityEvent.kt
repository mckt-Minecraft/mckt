package io.github.gaming32.mckt.packet.play.s2c

import io.github.gaming32.mckt.data.writeByte
import io.github.gaming32.mckt.data.writeInt
import io.github.gaming32.mckt.packet.Packet
import java.io.OutputStream

object EntityEvent {
    const val SPAWN_HONEY_PARTICLES: UByte = 53u

    object PlayerEvent {
        const val FINISH_ITEM_USE: UByte = 9u
        const val ENABLE_REDUCED_DEBUG_INFO: UByte = 22u
        const val DISABLE_REDUCED_DEBUG_INFO: UByte = 23u
        const val SET_OP_LEVEL_0: UByte = 24u
        const val SET_OP_LEVEL_1: UByte = 25u
        const val SET_OP_LEVEL_2: UByte = 26u
        const val SET_OP_LEVEL_3: UByte = 27u
        const val SET_OP_LEVEL_4: UByte = 28u
        const val SPAWN_CLOUD_PARTICLES: UByte = 43u
    }
}

data class EntityEventPacket(val entityId: Int, val event: UByte) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x1A
    }

    override fun write(out: OutputStream) {
        out.writeInt(entityId)
        out.writeByte(event.toInt())
    }
}
