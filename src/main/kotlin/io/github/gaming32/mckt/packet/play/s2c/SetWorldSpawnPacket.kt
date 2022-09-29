package io.github.gaming32.mckt.packet.play.s2c

import io.github.gaming32.mckt.data.writeBlockPosition
import io.github.gaming32.mckt.data.writeFloat
import io.github.gaming32.mckt.objects.BlockPosition
import io.github.gaming32.mckt.packet.Packet
import java.io.OutputStream

class SetWorldSpawnPacket(val location: BlockPosition, val yaw: Float) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x4D
    }

    override fun write(out: OutputStream) {
        out.writeBlockPosition(location)
        out.writeFloat(yaw)
    }
}
