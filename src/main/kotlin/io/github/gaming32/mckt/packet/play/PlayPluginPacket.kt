package io.github.gaming32.mckt.packet.play

import io.github.gaming32.mckt.data.MinecraftOutputStream
import io.github.gaming32.mckt.data.encodeData
import io.github.gaming32.mckt.data.readIdentifier
import io.github.gaming32.mckt.objects.Identifier
import io.github.gaming32.mckt.packet.Packet
import java.io.InputStream

data class PlayPluginPacket(val channel: Identifier, val data: ByteArray) : Packet(S2C_TYPE) {
    companion object {
        const val S2C_TYPE = 0x16
        const val C2S_TYPE = 0x0D
    }

    constructor(inp: InputStream) : this(
        inp.readIdentifier(),
        inp.readBytes()
    )

    override fun write(out: MinecraftOutputStream) {
        out.writeIdentifier(channel)
        out.write(data)
    }
}

inline fun PlayPluginPacket(channel: Identifier, builder: MinecraftOutputStream.() -> Unit) =
    PlayPluginPacket(channel, encodeData(builder))
