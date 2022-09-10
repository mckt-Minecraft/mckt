package io.github.gaming32.mckt.packet.play.c2s

import io.github.gaming32.mckt.objects.BlockPosition
import io.github.gaming32.mckt.objects.Direction
import io.github.gaming32.mckt.packet.MinecraftInputStream
import io.github.gaming32.mckt.packet.MinecraftOutputStream
import io.github.gaming32.mckt.packet.Packet
import io.github.gaming32.mckt.packet.readVarInt

class UseItemOnBlockPacket(
    val offhand: Boolean,
    val location: BlockPosition,
    val face: Direction,
    val cursorX: Float,
    val cursorY: Float,
    val cursorZ: Float,
    val insideBlock: Boolean,
    val sequence: Int = 0
) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x31
    }

    constructor(inp: MinecraftInputStream) : this(
        inp.readBoolean(),
        inp.readBlockPosition(),
        Direction.values()[inp.readVarInt()],
        inp.readFloat(),
        inp.readFloat(),
        inp.readFloat(),
        inp.readBoolean(),
        inp.readVarInt()
    )

    override fun write(out: MinecraftOutputStream) {
        out.writeBoolean(offhand)
        out.writeBlockPosition(location)
        out.writeVarInt(face.ordinal)
        out.writeFloat(cursorX)
        out.writeFloat(cursorY)
        out.writeFloat(cursorZ)
        out.writeBoolean(insideBlock)
        out.writeVarInt(sequence)
    }
}