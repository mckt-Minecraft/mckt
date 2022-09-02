package io.github.gaming32.mckt.packet.status

import io.github.gaming32.mckt.packet.MinecraftInputStream
import io.github.gaming32.mckt.packet.MinecraftOutputStream
import io.github.gaming32.mckt.packet.Packet

class PingPacket(val payload: Long) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x01
    }

    constructor(inp: MinecraftInputStream) : this(inp.readLong())

    override fun write(out: MinecraftOutputStream) = out.writeLong(payload)
}
