package io.github.gaming32.mckt.packet.play.c2s

import io.github.gaming32.mckt.packet.MinecraftInputStream
import io.github.gaming32.mckt.packet.MinecraftOutputStream
import io.github.gaming32.mckt.packet.Packet
import io.github.gaming32.mckt.packet.readVarInt

data class CommandCompletionsRequestPacket(val requestId: Int, val command: String) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x09
    }

    constructor(inp: MinecraftInputStream) : this(
        inp.readVarInt(),
        inp.readString(32500)
    )

    override fun write(out: MinecraftOutputStream) {
        out.writeVarInt(requestId)
        out.writeString(command, 32500)
    }
}
