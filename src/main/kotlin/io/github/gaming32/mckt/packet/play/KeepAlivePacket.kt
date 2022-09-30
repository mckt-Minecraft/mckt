package io.github.gaming32.mckt.packet.play

import io.github.gaming32.mckt.data.readLong
import io.github.gaming32.mckt.data.writeLong
import io.github.gaming32.mckt.packet.Packet
import java.io.InputStream
import java.io.OutputStream

data class KeepAlivePacket(val id: Long) : Packet(S2C_TYPE) {
    companion object {
        const val S2C_TYPE = 0x20
        const val C2S_TYPE = 0x12
    }

    constructor(inp: InputStream) : this(inp.readLong())

    override fun write(out: OutputStream) = out.writeLong(id)
}
