package io.github.gaming32.mckt.packet.play.c2s

import io.github.gaming32.mckt.data.readVarInt
import io.github.gaming32.mckt.data.writeVarInt
import io.github.gaming32.mckt.packet.Packet
import java.io.InputStream
import java.io.OutputStream

data class PlayerCommandPacket(val entityId: Int, val action: Int, val horseJumpBoost: Int = 0) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x1E
        const val START_SNEAKING = 0
        const val STOP_SNEAKING = 1
        const val LEAVE_BED = 2
        const val START_SPRINTING = 3
        const val STOP_SPRINTING = 4
        const val START_HORSE_JUMP = 5
        const val END_HORSE_JUMP = 6
        const val OPEN_HORSE_INVENTORY = 7
        const val START_FALL_FLYING = 8
    }

    constructor(inp: InputStream) : this(
        inp.readVarInt(),
        inp.readVarInt(),
        inp.readVarInt()
    )

    override fun write(out: OutputStream) {
        out.writeVarInt(entityId)
        out.writeVarInt(action)
        out.writeVarInt(horseJumpBoost)
    }
}
