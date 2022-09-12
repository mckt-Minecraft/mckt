package io.github.gaming32.mckt.packet.play.s2c

import io.github.gaming32.mckt.packet.MinecraftOutputStream
import io.github.gaming32.mckt.packet.Packet

data class AcknowledgeBlockChangePacket(val sequence: Int) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x05
    }

    override fun write(out: MinecraftOutputStream) = out.writeVarInt(sequence)
}
