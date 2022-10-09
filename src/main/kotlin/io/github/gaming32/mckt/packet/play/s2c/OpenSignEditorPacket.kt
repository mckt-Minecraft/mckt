package io.github.gaming32.mckt.packet.play.s2c

import io.github.gaming32.mckt.data.writeBlockPosition
import io.github.gaming32.mckt.objects.BlockPosition
import io.github.gaming32.mckt.packet.Packet
import java.io.OutputStream

data class OpenSignEditorPacket(val location: BlockPosition) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x2E
    }

    override fun write(out: OutputStream) = out.writeBlockPosition(location)
}
