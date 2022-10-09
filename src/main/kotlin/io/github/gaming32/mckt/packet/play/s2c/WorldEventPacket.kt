package io.github.gaming32.mckt.packet.play.s2c

import io.github.gaming32.mckt.data.writeBlockPosition
import io.github.gaming32.mckt.data.writeBoolean
import io.github.gaming32.mckt.data.writeInt
import io.github.gaming32.mckt.objects.BlockPosition
import io.github.gaming32.mckt.packet.Packet
import java.io.OutputStream

class WorldEventPacket(
    val event: Int,
    val pos: BlockPosition,
    val data: Int = 0,
    val absoluteVolume: Boolean = false
) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x22

        const val CLOSE_METAL_DOOR = 1005
        const val CLOSE_DOOR = 1006
        const val OPEN_TRAPDOOR = 1007
        const val OPEN_METAL_DOOR = 1011
        const val OPEN_DOOR = 1012
        const val CLOSE_TRAPDOOR = 1013
        const val CLOSE_METAL_TRAPDOOR = 1036
        const val OPEN_METAL_TRAPDOOR = 1037
        const val USE_BONEMEAL = 1505
        const val BREAK_BLOCK = 2001
    }

    override fun write(out: OutputStream) {
        out.writeInt(event)
        out.writeBlockPosition(pos)
        out.writeInt(data)
        out.writeBoolean(absoluteVolume)
    }
}
