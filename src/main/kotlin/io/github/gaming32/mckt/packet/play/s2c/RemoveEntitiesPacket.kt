package io.github.gaming32.mckt.packet.play.s2c

import io.github.gaming32.mckt.data.writeVarIntArray
import io.github.gaming32.mckt.packet.Packet
import java.io.OutputStream

class RemoveEntitiesPacket(vararg val entities: Int) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x3B
    }

    override fun write(out: OutputStream) {
        out.writeVarIntArray(entities)
    }

    override fun toString() = "RemoveEntitiesPacket(entities=${entities.contentToString()})"
}
