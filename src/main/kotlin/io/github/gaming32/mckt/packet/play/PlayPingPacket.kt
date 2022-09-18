package io.github.gaming32.mckt.packet.play

import io.github.gaming32.mckt.data.MinecraftOutputStream
import io.github.gaming32.mckt.data.readInt
import io.github.gaming32.mckt.packet.Packet
import java.io.InputStream

data class PlayPingPacket(val id: Int) : Packet(S2C_TYPE) {
    companion object {
        const val S2C_TYPE = 0x2F
        const val C2S_TYPE = 0x20
    }

    constructor(inp: InputStream) : this(inp.readInt())

    override fun write(out: MinecraftOutputStream) = out.writeInt(id)
}
