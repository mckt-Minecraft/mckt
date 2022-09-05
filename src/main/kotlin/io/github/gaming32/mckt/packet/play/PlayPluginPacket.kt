package io.github.gaming32.mckt.packet.play

import io.github.gaming32.mckt.objects.Identifier
import io.github.gaming32.mckt.packet.MinecraftInputStream
import io.github.gaming32.mckt.packet.MinecraftOutputStream
import io.github.gaming32.mckt.packet.Packet
import java.io.ByteArrayOutputStream

class PlayPluginPacket(val channel: Identifier, val data: ByteArray) : Packet(S2C_TYPE) {
    companion object {
        const val S2C_TYPE = 0x16
        const val C2S_TYPE = 0x0D
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

inline fun PlayPluginPacket(channel: Identifier, builder: MinecraftOutputStream.() -> Unit) = PlayPluginPacket(
    channel,
    ByteArrayOutputStream().also { MinecraftOutputStream(it).builder() }.toByteArray()
)
