package io.github.gaming32.mckt.packet.play.c2s

import io.github.gaming32.mckt.data.readUByte
import io.github.gaming32.mckt.data.writeByte
import io.github.gaming32.mckt.packet.Packet
import java.io.InputStream
import java.io.OutputStream

data class ServerboundPlayerAbilitiesPacket(val flying: Boolean) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x1C
    }

    constructor(inp: InputStream) : this((inp.readUByte().toInt() and 0x02) != 0)

    override fun write(out: OutputStream) = out.writeByte(if (flying) 0x02 else 0x00)
}
