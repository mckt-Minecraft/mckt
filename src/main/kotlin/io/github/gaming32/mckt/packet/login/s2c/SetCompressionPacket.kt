package io.github.gaming32.mckt.packet.login.s2c

import io.github.gaming32.mckt.data.writeVarInt
import io.github.gaming32.mckt.packet.Packet
import java.io.OutputStream

data class SetCompressionPacket(val threshold: Int) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x03
    }

    override fun write(out: OutputStream) = out.writeVarInt(threshold)
}
