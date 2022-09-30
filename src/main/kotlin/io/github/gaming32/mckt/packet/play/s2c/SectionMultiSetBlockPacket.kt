package io.github.gaming32.mckt.packet.play.s2c

import io.github.gaming32.mckt.GlobalPalette.BLOCKSTATE_TO_ID
import io.github.gaming32.mckt.data.writeArray
import io.github.gaming32.mckt.data.writeBoolean
import io.github.gaming32.mckt.data.writeLong
import io.github.gaming32.mckt.data.writeVarLong
import io.github.gaming32.mckt.objects.BlockPosition
import io.github.gaming32.mckt.objects.BlockState
import io.github.gaming32.mckt.packet.Packet
import java.io.OutputStream

data class SectionMultiSetBlockPacket(
    val section: BlockPosition,
    val actions: Map<BlockPosition, BlockState>
) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x40
    }

    override fun write(out: OutputStream) {
        out.writeLong(
            (section.x.toLong() and 0x3FFFFF shl 42) or
                (section.y.toLong() and 0xFFFFF) or
                (section.z.toLong() and 0x3FFFFF shl 20)
        )
        out.writeBoolean(false)
        out.writeArray(actions) { location, state ->
            writeVarLong(
                (BLOCKSTATE_TO_ID.getInt(state).toLong() shl 12) or
                    (location.x.toLong() shl 8) or
                    (location.z.toLong() shl 4) or
                    location.y.toLong()
            )
        }
    }
}
