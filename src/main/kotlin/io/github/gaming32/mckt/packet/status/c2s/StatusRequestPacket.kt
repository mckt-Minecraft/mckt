package io.github.gaming32.mckt.packet.status.c2s

import io.github.gaming32.mckt.packet.MinecraftInputStream
import io.github.gaming32.mckt.packet.MinecraftOutputStream
import io.github.gaming32.mckt.packet.Packet

class StatusRequestPacket() : Packet(TYPE) {
    companion object {
        const val TYPE = 0x00
    }

    constructor(inp: MinecraftInputStream) : this()

    override fun write(out: MinecraftOutputStream) = Unit
}
