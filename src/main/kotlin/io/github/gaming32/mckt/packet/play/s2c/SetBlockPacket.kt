package io.github.gaming32.mckt.packet.play.s2c

import io.github.gaming32.mckt.GlobalPalette.BLOCKSTATE_TO_ID
import io.github.gaming32.mckt.World
import io.github.gaming32.mckt.data.writeBlockPosition
import io.github.gaming32.mckt.data.writeVarInt
import io.github.gaming32.mckt.objects.BlockPosition
import io.github.gaming32.mckt.objects.BlockState
import io.github.gaming32.mckt.packet.Packet
import java.io.OutputStream

data class SetBlockPacket(val location: BlockPosition, val blockState: BlockState) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x09
    }

    constructor(world: World, location: BlockPosition) : this(location, world.getBlockImmediate(location))

    override fun write(out: OutputStream) {
        out.writeBlockPosition(location)
        out.writeVarInt(BLOCKSTATE_TO_ID.getInt(blockState).takeIf { it != -1 }
            ?: throw IllegalArgumentException("Not network syncable: $blockState"))
    }
}
