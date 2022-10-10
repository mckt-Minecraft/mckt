package io.github.gaming32.mckt.packet.play.s2c

import io.github.gaming32.mckt.data.writeBoolean
import io.github.gaming32.mckt.packet.Packet
import java.io.OutputStream

data class ToggleChatPreviewPacket(val enablePreview: Boolean) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x4E
    }

    override fun write(out: OutputStream) = out.writeBoolean(enablePreview)
}
