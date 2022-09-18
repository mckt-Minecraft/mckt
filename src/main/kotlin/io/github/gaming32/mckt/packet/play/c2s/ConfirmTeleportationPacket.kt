package io.github.gaming32.mckt.packet.play.c2s

import io.github.gaming32.mckt.data.MinecraftOutputStream
import io.github.gaming32.mckt.data.readVarInt
import io.github.gaming32.mckt.packet.Packet
import java.io.InputStream

data class ConfirmTeleportationPacket(val teleportId: Int) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x00
    }

    constructor(inp: InputStream) : this(inp.readVarInt())

    override fun write(out: MinecraftOutputStream) = out.writeVarInt(teleportId)
}
