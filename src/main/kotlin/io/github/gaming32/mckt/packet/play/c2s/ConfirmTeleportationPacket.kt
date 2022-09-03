package io.github.gaming32.mckt.packet.play.c2s

import io.github.gaming32.mckt.packet.MinecraftInputStream
import io.github.gaming32.mckt.packet.MinecraftOutputStream
import io.github.gaming32.mckt.packet.Packet

class ConfirmTeleportationPacket(val teleportId: Int) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x00
    }

    constructor(inp: MinecraftInputStream) : this(inp.readVarInt())

    override fun write(out: MinecraftOutputStream) = out.writeVarInt(teleportId)
}
