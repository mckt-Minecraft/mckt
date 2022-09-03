package io.github.gaming32.mckt.packet.play.c2s

import io.github.gaming32.mckt.objects.Identifier
import io.github.gaming32.mckt.packet.MinecraftInputStream
import io.github.gaming32.mckt.packet.MinecraftOutputStream
import io.github.gaming32.mckt.packet.Packet

class PlayPluginC2SPacket(val channel: Identifier, val data: ByteArray) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x0D
    }

    constructor(inp: MinecraftInputStream) : this(
        inp.readIdentifier(),
        inp.readBytes()
    )

    override fun write(out: MinecraftOutputStream) {
        out.writeIdentifier(channel)
        out.write(data)
    }
}
