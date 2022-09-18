package io.github.gaming32.mckt.packet.play.s2c

import io.github.gaming32.mckt.data.writeInt
import io.github.gaming32.mckt.packet.Packet
import java.io.OutputStream

class UnloadChunkPacket(val x: Int, val z: Int) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x1C
    }

    override fun write(out: OutputStream) {
        out.writeInt(x)
        out.writeInt(z)
    }
}
