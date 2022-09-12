package io.github.gaming32.mckt.packet.play.s2c

import io.github.gaming32.mckt.packet.MinecraftOutputStream
import io.github.gaming32.mckt.packet.Packet
import net.benwoodworth.knbt.buildNbtCompound
import net.benwoodworth.knbt.put
import java.util.*

data class ChunkAndLightDataPacket(
    val x: Int, val z: Int,
    val chunk: ByteArray
) : Packet(TYPE) {
    companion object {
        val TYPE = 0x21
        private val EMPTY_BIT_SET = BitSet(256)
        private val FULL_BIT_SET = BitSet.valueOf(LongArray(4) { -1L })
        private val HEIGHTMAP = LongArray(52)
    }

    override fun write(out: MinecraftOutputStream) {
        out.writeInt(x)
        out.writeInt(z)
        out.writeNbtTag(buildNbtCompound("") {
            put("MOTION_BLOCKING", HEIGHTMAP)
        })
        out.writeVarInt(chunk.size)
        out.write(chunk)
        out.writeVarInt(0) // Number of block entities
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
