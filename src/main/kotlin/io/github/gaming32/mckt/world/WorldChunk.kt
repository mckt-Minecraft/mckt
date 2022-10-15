package io.github.gaming32.mckt.world

import io.github.gaming32.mckt.blocks.BlockEntityProvider
import io.github.gaming32.mckt.blocks.entities.BlockEntities
import io.github.gaming32.mckt.blocks.entities.BlockEntity
import io.github.gaming32.mckt.cast
import io.github.gaming32.mckt.data.writeByte
import io.github.gaming32.mckt.data.writeShort
import io.github.gaming32.mckt.data.writeVarInt
import io.github.gaming32.mckt.getLogger
import io.github.gaming32.mckt.nbt.NbtCompound
import io.github.gaming32.mckt.nbt.buildNbtCompound
import io.github.gaming32.mckt.nbt.put
import io.github.gaming32.mckt.nbt.putNbtList
import io.github.gaming32.mckt.objects.BlockPosition
import io.github.gaming32.mckt.objects.BlockState
import java.io.OutputStream
import java.util.*

class WorldChunk(val region: WorldRegion, val xInRegion: Int, val zInRegion: Int) : BlockAccess {
    companion object {
        private val LOGGER = getLogger()
    }

    val world get() = region.world
    val x get() = (region.x shl 5) + xInRegion
    val z get() = (region.z shl 5) + zInRegion

    private val sections = arrayOfNulls<ChunkSection>(254)
    internal val blockEntities = mutableMapOf<BlockPosition, BlockEntity<*>>()
    internal var ready = false

    fun getSection(y: Int): ChunkSection? = sections[y + 127]

    override fun getBlockImmediate(x: Int, y: Int, z: Int): BlockState {
        if (y < -2032 || y > 2031 || x !in 0..15 || z !in 0..15) return Blocks.AIR
        val section = sections[(y shr 4) + 127] ?: return Blocks.AIR
        return section.getBlock(x, y and 15, z)
    }

    override fun setBlockImmediate(x: Int, y: Int, z: Int, block: BlockState): Boolean {
        if (y !in -2032..2031 || x !in 0..15 || z !in 0..15) return false
        val old: BlockState
        synchronized(this) {
            var section = sections[(y shr 4) + 127]
            if (section == null) {
                if (block == Blocks.AIR) return true
                section = ChunkSection(this, y shr 4)
                sections[(y shr 4) + 127] = section
            }
            old = section.setBlock(x, y and 15, z, block)
            if (section.blockCount == 0) { // The section is now empty
                sections[(y shr 4) + 127] = null
            }
        }
        val pos = BlockPosition((this.x shl 4) + x, y, (this.z shl 4) + z)
        if (old != block) {
            old.onStateReplaced(world, pos, block, false)
            if (block.hasBlockEntity(world.server)) {
                val relPos = BlockPosition(x, y, z)
                val entity = getBlockEntity(relPos)
                if (entity == null) {
                    addBlockEntity(
                        block.getHandler(world.server)
                            .cast<BlockEntityProvider<*>>()
                            .createBlockEntity(pos, block)
                    )
                } else {
                    entity.cachedState = block
                }
            }
        }
        if (ready) {
            world.dirtyBlocks.add(pos)
        }
        return true
    }

    fun addBlockEntity(entity: BlockEntity<*>) {
        blockEntities[entity.pos - BlockPosition(x shl 4, 0, z shl 4)] = entity
    }

    fun getBlockEntity(pos: BlockPosition) = blockEntities[pos]

    fun removeBlockEntity(pos: BlockPosition) {
        blockEntities.remove(pos)
    }

    internal fun toNbt() = synchronized(this) {
        buildNbtCompound {
            val sectionsPresent = BitSet(sections.size)
            putNbtList<NbtCompound>("Sections") {
                sections.forEachIndexed { i, section ->
                    if (section != null && section.blockCount > 0) {
                        sectionsPresent.set(i)
                        add(section.toNbt())
                    }
                }
            }
            put("SectionsPresent", sectionsPresent.toLongArray())
            putNbtList<NbtCompound>("BlockEntities") {
                blockEntities.values.forEach { add(it.toIdentifiedLocatedNbt()) }
            }
        }
    }

    internal fun fromNbt(nbt: NbtCompound) {
        val sectionsPresent = BitSet.valueOf(nbt.getLongArray("SectionsPresent"))
        val sectionsNbt = nbt.getList<NbtCompound>("Sections").content
        var dataIndex = 0
        repeat(254) { i ->
            if (sectionsPresent[i]) {
                sections[i] = ChunkSection(this, i - 127).also { it.fromNbt(sectionsNbt[dataIndex++]) }
            } else {
                sections[i] = null
            }
        }
        blockEntities.clear()
        val chunkOrigin = BlockPosition(x shl 4, 0, z shl 4)
        nbt.getList<NbtCompound>("BlockEntities").forEach {
            val location = BlockEntities.blockPositionFromNbt(it)
            val relativeLocation = location - chunkOrigin
            val entity = BlockEntities.createFromNbt(location, getBlockImmediate(relativeLocation), it)
            if (entity == null) {
                LOGGER.warn("Skipped block entity at $location")
            } else {
                blockEntities[relativeLocation] = entity
            }
        }
        ready = true
    }

    fun networkEncode(out: OutputStream) {
        for (section in sections) {
            if (section == null) {
                out.writeShort(0) // Block count
                out.writeByte(0) // Blocks: Bits per entry
                out.writeVarInt(0) // Blocks: Air ID
                out.writeVarInt(0) // Blocks: Data size
            } else {
                synchronized(this) {
                    out.writeShort(section.blockCount)
                    section.data.encode(out)
                }
            }
            out.writeByte(0) // Biomes: Bits per entry
            out.writeVarInt(0) // Biomes: Plains ID
            out.writeVarInt(0) // Biomes: Data size
        }
    }
}