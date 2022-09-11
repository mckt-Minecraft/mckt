package io.github.gaming32.mckt.packet.play.s2c

import io.github.gaming32.mckt.packet.MinecraftOutputStream
import io.github.gaming32.mckt.packet.Packet

class ClientboundSetHeldItemPacket(val slot: Int) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x4A
    }

    override fun write(out: MinecraftOutputStream) = out.writeByte(slot)
}
