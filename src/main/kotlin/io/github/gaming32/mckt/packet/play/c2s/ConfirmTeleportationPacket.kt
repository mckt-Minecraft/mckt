package io.github.gaming32.mckt.packet.play.c2s

import io.github.gaming32.mckt.data.readVarInt
import io.github.gaming32.mckt.data.writeVarInt
import io.github.gaming32.mckt.packet.Packet
import java.io.InputStream
import java.io.OutputStream

data class ConfirmTeleportationPacket(val teleportId: Int) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x00
    }

    constructor(inp: InputStream) : this(inp.readVarInt())

    override fun write(out: OutputStream) = out.writeVarInt(teleportId)
}
