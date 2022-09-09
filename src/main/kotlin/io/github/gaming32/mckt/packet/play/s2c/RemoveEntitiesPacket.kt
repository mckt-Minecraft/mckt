package io.github.gaming32.mckt.packet.play.s2c

import io.github.gaming32.mckt.packet.MinecraftOutputStream
import io.github.gaming32.mckt.packet.Packet

class RemoveEntitiesPacket(vararg val entities: Int) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x3B
    }

    override fun write(out: MinecraftOutputStream) {
        out.writeVarInt(entities.size)
        entities.forEach { out.writeVarInt(it) }
    }
}
