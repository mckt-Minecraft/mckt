package io.github.gaming32.mckt.packet.play.s2c

import io.github.gaming32.mckt.data.writeBoolean
import io.github.gaming32.mckt.data.writeOptionalString
import io.github.gaming32.mckt.data.writeOptionalText
import io.github.gaming32.mckt.packet.Packet
import net.kyori.adventure.text.Component
import java.io.OutputStream

data class ServerDataPacket(
    val motd: Component?,
    val icon: String?,
    val previewsChat: Boolean,
    val enforcesSecureChat: Boolean
) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x42
    }

    override fun write(out: OutputStream) {
        out.writeOptionalText(motd)
        out.writeOptionalString(icon, 32767)
        out.writeBoolean(previewsChat)
        out.writeBoolean(enforcesSecureChat)
    }
}
