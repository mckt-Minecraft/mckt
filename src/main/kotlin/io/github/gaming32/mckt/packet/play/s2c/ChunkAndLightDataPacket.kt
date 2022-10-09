package io.github.gaming32.mckt.packet.play.s2c

import io.github.gaming32.mckt.blocks.entities.BlockEntities.getMetadata
import io.github.gaming32.mckt.blocks.entities.BlockEntity
import io.github.gaming32.mckt.data.*
import io.github.gaming32.mckt.nbt.buildNbtCompound
import io.github.gaming32.mckt.nbt.put
import io.github.gaming32.mckt.objects.BlockPosition
import io.github.gaming32.mckt.packet.Packet
import java.io.OutputStream
import java.util.*

data class ChunkAndLightDataPacket(
    val x: Int, val z: Int,
    val blockEntities: Map<BlockPosition, BlockEntity<*>>,
    val chunk: ByteArray
) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x21
        private val EMPTY_BIT_SET = BitSet(256)
        private val FULL_BIT_SET = BitSet.valueOf(LongArray(4) { -1L })
        private val HEIGHTMAP = LongArray(52)
    }

    override fun write(out: OutputStream) {
        out.writeInt(x)
        out.writeInt(z)
        out.writeNbt(buildNbtCompound {
            put("MOTION_BLOCKING", HEIGHTMAP)
        })
        out.writeVarInt(chunk.size)
        out.write(chunk)
        out.writeArray(blockEntities) { pos, entity ->
            writeByte(pos.x shl 4 or pos.z)
            writeShort(pos.y)
            writeVarInt(entity.type.getMetadata()?.networkId
                ?: throw IllegalStateException("Unknown block entity ${entity.javaClass.simpleName}"))
            writeNbt(entity.initialNetworkSerialize())
        }
        out.writeBoolean(true) // Lighting: Trust edges
        out.writeBitSet(EMPTY_BIT_SET) // Skylight mask
        out.writeBitSet(EMPTY_BIT_SET) // Block light mask
        out.writeBitSet(FULL_BIT_SET) // Empty skylight mask
        out.writeBitSet(FULL_BIT_SET) // Empty block light mask
        out.writeVarInt(0) // Skylight data count
        out.writeVarInt(0) // Block light data count
    }

    override fun toString() = "ChunkAndLightDataPacket(x=$x, z=$z, chunk=...)"
}
