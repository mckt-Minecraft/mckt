package io.github.gaming32.mckt.packet.play.c2s

import io.github.gaming32.mckt.data.readString
import io.github.gaming32.mckt.data.readVarInt
import io.github.gaming32.mckt.data.writeString
import io.github.gaming32.mckt.data.writeVarInt
import io.github.gaming32.mckt.packet.Packet
import java.io.InputStream
import java.io.OutputStream

data class CommandCompletionsRequestPacket(val requestId: Int, val command: String) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x09
    }

    constructor(inp: InputStream) : this(
        inp.readVarInt(),
        inp.readString(32500)
    )

    override fun write(out: OutputStream) {
        out.writeVarInt(requestId)
        out.writeString(command, 32500)
    }
}
