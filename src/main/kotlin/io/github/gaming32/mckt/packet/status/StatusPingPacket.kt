package io.github.gaming32.mckt.packet.status

import io.github.gaming32.mckt.data.readLong
import io.github.gaming32.mckt.data.writeLong
import io.github.gaming32.mckt.packet.Packet
import java.io.InputStream
import java.io.OutputStream

data class StatusPingPacket(val payload: Long) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x01
    }

    constructor(inp: InputStream) : this(inp.readLong())

    override fun write(out: OutputStream) = out.writeLong(payload)
}
