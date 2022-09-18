package io.github.gaming32.mckt.packet.status

import io.github.gaming32.mckt.data.MinecraftOutputStream
import io.github.gaming32.mckt.data.readLong
import io.github.gaming32.mckt.packet.Packet
import java.io.InputStream

data class StatusPingPacket(val payload: Long) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x01
    }

    constructor(inp: InputStream) : this(inp.readLong())

    override fun write(out: MinecraftOutputStream) = out.writeLong(payload)
}
