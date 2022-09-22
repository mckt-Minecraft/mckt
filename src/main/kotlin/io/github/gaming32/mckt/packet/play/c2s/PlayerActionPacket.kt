package io.github.gaming32.mckt.packet.play.c2s

import io.github.gaming32.mckt.data.*
import io.github.gaming32.mckt.objects.BlockPosition
import io.github.gaming32.mckt.objects.Direction
import io.github.gaming32.mckt.packet.Packet
import java.io.InputStream
import java.io.OutputStream

data class PlayerActionPacket(
    val action: Action,
    val location: BlockPosition,
    val face: Direction,
    val sequence: Int = 0
) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x1D
    }

    enum class Action {
        START_DIGGING,
        CANCEL_DIGGING,
        FINISH_DIGGING,
        DROP_ITEM_STACK,
        DROP_ITEM,
        FINISH_ITEM_USE,
        SWAP_OFFHAND
    }

    constructor(inp: InputStream) : this(
        inp.readEnum(),
        inp.readBlockPosition(),
        inp.readByteEnum(),
        inp.readVarInt()
    )

    override fun write(out: OutputStream) {
        out.writeEnum(action)
        out.writeBlockPosition(location)
        out.writeByteEnum(face)
        out.writeVarInt(sequence)
    }
}
