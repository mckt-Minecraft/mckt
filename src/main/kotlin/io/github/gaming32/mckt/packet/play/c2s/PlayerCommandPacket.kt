package io.github.gaming32.mckt.packet.play.c2s

import io.github.gaming32.mckt.packet.MinecraftInputStream
import io.github.gaming32.mckt.packet.MinecraftOutputStream
import io.github.gaming32.mckt.packet.Packet
import io.github.gaming32.mckt.packet.readVarInt

class PlayerCommandPacket(val entityId: Int, val action: Int, val horseJumpBoost: Int = 0) : Packet(TYPE) {
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

    constructor(inp: MinecraftInputStream) : this(
        inp.readVarInt(),
        inp.readVarInt(),
        inp.readVarInt()
    )

    override fun write(out: MinecraftOutputStream) {
        out.writeVarInt(entityId)
        out.writeVarInt(action)
        out.writeVarInt(horseJumpBoost)
    }
}
