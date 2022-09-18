package io.github.gaming32.mckt.packet.play.s2c

import io.github.gaming32.mckt.data.writeByte
import io.github.gaming32.mckt.packet.Packet
import java.io.OutputStream

data class ClientboundSetHeldItemPacket(val slot: Int) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x4A
    }

    override fun write(out: OutputStream) = out.writeByte(slot)
}
