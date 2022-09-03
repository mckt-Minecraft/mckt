package io.github.gaming32.mckt.packet.play.s2c

import io.github.gaming32.mckt.packet.MinecraftOutputStream
import io.github.gaming32.mckt.packet.Packet
import net.kyori.adventure.text.Component

class PlayDisconnectPacket(val reason: Component) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x19
    }

    override fun write(out: MinecraftOutputStream) = out.writeText(reason)
}
