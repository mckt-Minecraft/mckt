package io.github.gaming32.mckt.packet.play.s2c

import io.github.gaming32.mckt.data.writeInt
import io.github.gaming32.mckt.data.writeOptionalText
import io.github.gaming32.mckt.packet.Packet
import net.kyori.adventure.text.Component
import java.io.OutputStream

data class ClientboundChatPreviewPacket(val query: Int, val message: Component?) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x0C
    }

    override fun write(out: OutputStream) {
        out.writeInt(query)
        out.writeOptionalText(message)
    }
}
