package io.github.gaming32.mckt.world

import io.github.gaming32.mckt.cast
import io.github.gaming32.mckt.data.writeVarInt
import io.github.gaming32.mckt.nbt.*
import io.github.gaming32.mckt.objects.BlockPosition
import io.github.gaming32.mckt.objects.BlockState
import io.github.gaming32.mckt.util.PalettedStorage
import io.github.gaming32.mckt.util.SimpleBitStorage

class ChunkSection(val chunk: WorldChunk, val y: Int) {
    val world get() = chunk.region.world
    val x get() = chunk.x
    val z get() = chunk.z
    val region get() = chunk.region
    val xInRegion get() = chunk.xInRegion
    val zInRegion get() = chunk.zInRegion

    internal val data = PalettedStorage(4096, Blocks.AIR) { writeVarInt(it.globalId) }

    var blockCount = 0
        private set

    private fun getBlockIndex(x: Int, y: Int, z: Int) = (y shl 8) + (z shl 4) + x

    fun getBlock(x: Int, y: Int, z: Int) = data[getBlockIndex(x, y, z)]

    fun getBlock(pos: BlockPosition) = getBlock(pos.x, pos.y, pos.z)

    internal fun setBlock(x: Int, y: Int, z: Int, block: BlockState): BlockState {
        val index = getBlockIndex(x, y, z)
        val old = data[index]
        data[index] = block
        if (block == Blocks.AIR) {
            if (old != Blocks.AIR) {
                blockCount--
            }
        } else if (old == Blocks.AIR) {
            blockCount++
        }
        return old
    }

    internal fun toNbt() = buildNbtCompound {
        data.compact()
        put("BlockCount", blockCount)
        putNbtList<NbtString>("Palette") {
            for (state in data.paletteItems) {
                add(state.toString())
            }
        }
        putNbtCompound("Blocks") {
            put("bits", data.storage.bits)
            put("data", data.storage.data)
        }
    }

    internal fun fromNbt(nbt: NbtCompound) {
        blockCount = nbt.getInt("BlockCount")
        data.setPaletteItems(nbt.getList<NbtElement<*, *>>("Palette").map {
            if (it is NbtString) {
                BlockState.parse(it.value)
            } else {
                BlockState.fromMap(it.cast<Map<String, NbtString>>().entries.associate { (key, value) ->
                    key to value.value
                })
            }
        })
        val blocks = nbt.getCompound("Blocks")
        data.storage = SimpleBitStorage(blocks.getInt("bits"), 4096, blocks.getLongArray("data"))
    }
}