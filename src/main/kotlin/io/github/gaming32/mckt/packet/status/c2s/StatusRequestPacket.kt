package io.github.gaming32.mckt.packet.status.c2s

import io.github.gaming32.mckt.data.MinecraftOutputStream
import io.github.gaming32.mckt.packet.Packet
import java.io.InputStream

class StatusRequestPacket() : Packet(TYPE) {
    companion object {
        const val TYPE = 0x00
    }

    constructor(inp: InputStream) : this()

    override fun write(out: MinecraftOutputStream) = Unit

    override fun toString() = "StatusRequestPacket()"
}
