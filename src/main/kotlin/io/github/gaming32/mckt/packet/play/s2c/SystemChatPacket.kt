package io.github.gaming32.mckt.packet.play.s2c

import io.github.gaming32.mckt.packet.MinecraftOutputStream
import io.github.gaming32.mckt.packet.Packet
import net.kyori.adventure.text.Component

class SystemChatPacket(val text: Component, val actionBar: Boolean = false) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x62
    }

    override fun write(out: MinecraftOutputStream) {
        out.writeText(text)
        out.writeBoolean(actionBar)
    }
}
