package io.github.gaming32.mckt.packet.login.c2s

import io.github.gaming32.mckt.data.readBoolean
import io.github.gaming32.mckt.data.readVarInt
import io.github.gaming32.mckt.data.writeBoolean
import io.github.gaming32.mckt.data.writeVarInt
import io.github.gaming32.mckt.packet.Packet
import java.io.InputStream
import java.io.OutputStream

class LoginPluginResponsePacket(val messageId: Int, val understood: Boolean, val data: ByteArray) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x02
    }

    constructor(inp: InputStream) : this(
        inp.readVarInt(),
        inp.readBoolean(),
        inp.readBytes()
    )

    override fun write(out: OutputStream) {
        out.writeVarInt(messageId)
        out.writeBoolean(understood)
        out.write(data)
    }
}
