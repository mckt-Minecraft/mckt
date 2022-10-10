package io.github.gaming32.mckt.packet.play.c2s

import io.github.gaming32.mckt.data.readInt
import io.github.gaming32.mckt.data.readString
import io.github.gaming32.mckt.data.writeInt
import io.github.gaming32.mckt.data.writeString
import io.github.gaming32.mckt.packet.Packet
import java.io.InputStream
import java.io.OutputStream

data class ServerboundChatPreviewPacket(val query: Int, val message: String) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x06
    }

    constructor(inp: InputStream) : this(
        inp.readInt(),
        inp.readString(256)
    )

    override fun write(out: OutputStream) {
        out.writeInt(query)
        out.writeString(message, 256)
    }
}
