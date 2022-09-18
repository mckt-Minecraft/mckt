package io.github.gaming32.mckt.packet.login.s2c

import io.github.gaming32.mckt.data.MinecraftOutputStream
import io.github.gaming32.mckt.packet.Packet

data class SetCompressionPacket(val threshold: Int) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x03
    }

    override fun write(out: MinecraftOutputStream) = out.writeVarInt(threshold)
}
