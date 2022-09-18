package io.github.gaming32.mckt.packet.play.c2s

import io.github.gaming32.mckt.data.*
import io.github.gaming32.mckt.objects.BlockPosition
import io.github.gaming32.mckt.objects.Direction
import io.github.gaming32.mckt.packet.Packet
import java.io.InputStream

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
        inp.readVarIntEnum(),
        inp.readBlockPosition(),
        inp.readUByteEnum(),
        inp.readVarInt()
    )

    override fun write(out: MinecraftOutputStream) {
        out.writeVarInt(action.ordinal)
        out.writeBlockPosition(location)
        out.writeByte(face.ordinal)
        out.writeVarInt(sequence)
    }
}
