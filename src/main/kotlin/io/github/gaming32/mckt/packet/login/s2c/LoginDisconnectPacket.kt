package io.github.gaming32.mckt.packet.login.s2c

import io.github.gaming32.mckt.packet.MinecraftOutputStream
import io.github.gaming32.mckt.packet.Packet
import net.kyori.adventure.text.Component

class LoginDisconnectPacket(val reason: Component) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x00
    }

    override fun write(out: MinecraftOutputStream) = out.writeText(reason)
}
