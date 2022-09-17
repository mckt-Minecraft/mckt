package io.github.gaming32.mckt.packet.play.s2c

import io.github.gaming32.mckt.packet.MinecraftOutputStream
import io.github.gaming32.mckt.packet.Packet

class UnloadChunkPacket(val x: Int, val z: Int) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x1C
    }

    override fun write(out: MinecraftOutputStream) {
        out.writeInt(x)
        out.writeInt(z)
    }
}
