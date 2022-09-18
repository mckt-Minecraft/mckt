package io.github.gaming32.mckt.packet.login.s2c

import io.github.gaming32.mckt.data.writeText
import io.github.gaming32.mckt.packet.Packet
import net.kyori.adventure.text.Component
import java.io.OutputStream

data class LoginDisconnectPacket(val reason: Component) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x00
    }

    override fun write(out: OutputStream) = out.writeText(reason)
}
