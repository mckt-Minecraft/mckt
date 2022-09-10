package io.github.gaming32.mckt.packet.play.s2c

import io.github.gaming32.mckt.GLOBAL_PALETTE
import io.github.gaming32.mckt.objects.BlockPosition
import io.github.gaming32.mckt.objects.Identifier
import io.github.gaming32.mckt.packet.MinecraftOutputStream
import io.github.gaming32.mckt.packet.Packet

class SetBlockPacket(val location: BlockPosition, val id: Identifier?) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x09
    }

    override fun write(out: MinecraftOutputStream) {
        out.writeBlockPosition(location)
        out.writeVarInt(GLOBAL_PALETTE[id] ?: throw IllegalArgumentException("Unknown block ID: $id"))
    }
}
