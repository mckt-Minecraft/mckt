package io.github.gaming32.mckt.world

import io.github.gaming32.mckt.getLogger
import io.github.gaming32.mckt.nbt.*
import io.github.gaming32.mckt.objects.BlockPosition
import io.github.gaming32.mckt.objects.BlockState
import io.github.gaming32.mckt.world.gen.GeneratorArgs
import kotlinx.coroutines.Job
import java.io.File
import java.io.FileNotFoundException
import java.util.*
import kotlin.io.path.moveTo

class WorldRegion(val world: World, val x: Int, val z: Int) : AutoCloseable, BlockAccess {
    companion object {
        private val LOGGER = getLogger()
    }

    val regionFile = File(world.regionsDir, "region_${x}_${z}.nbt")

    internal var loadJob: Job? = null

    private val chunks = arrayOfNulls<WorldChunk>(32 * 32)

    fun load() {
        try {
            regionFile.inputStream().use { fromNbt(readCompressedNbt(it)) }
        } catch (_: FileNotFoundException) {
        } catch (e: Exception) {
            LOGGER.error("Failed to load region from file", e)
        }
    }

    fun getChunk(x: Int, z: Int): WorldChunk? = chunks[(x shl 5) + z]

    suspend fun getChunkOrElse(x: Int, z: Int, generate: suspend (GeneratorArgs) -> Unit = {}): WorldChunk {
        var chunk = chunks[x * 32 + z]
        if (chunk == null) {
            chunk = WorldChunk(this, x, z)
            chunks[x * 32 + z] = chunk
            generate(GeneratorArgs(chunk))
            chunk.ready = true
        }
        return chunk
    }

    suspend fun getChunkOrGenerate(x: Int, z: Int) = getChunkOrElse(x, z, world.worldGenerator::generateChunkThreaded)

    override fun getBlockImmediate(x: Int, y: Int, z: Int) =
        chunks[(x shr 4 shl 5) + (z shr 4)]?.getBlockImmediate(x and 15, y, z and 15) ?: Blocks.AIR

    suspend fun getBlockOrGenerate(x: Int, y: Int, z: Int) =
        getChunkOrGenerate(x shr 4, z shr 4).getBlockImmediate(x and 15, y, z and 15)

    suspend fun getBlockOrGenerate(pos: BlockPosition) = getBlockOrGenerate(pos.x, pos.y, pos.z)

    override fun setBlockImmediate(x: Int, y: Int, z: Int, block: BlockState) =
        chunks[(x shr 4 shl 5) + (z shr 4)]?.setBlockImmediate(x and 15, y, z and 15, block) ?: false

    private fun toNbt() = buildNbtCompound {
        val chunksPresent = BitSet(chunks.size)
        putNbtList<NbtCompound>("Chunks") {
            chunks.toList().forEachIndexed { i, chunk ->
                if (chunk != null) {
                    chunksPresent.set(i)
                    add(chunk.toNbt())
                }
            }
        }
        put("ChunksPresent", chunksPresent.toLongArray())
    }

    private fun fromNbt(nbt: NbtCompound) {
        val chunksPresent = BitSet.valueOf(nbt.getLongArray("ChunksPresent"))
        val chunksNbt = nbt.getList<NbtCompound>("Chunks").content
        var dataIndex = 0
        repeat(32) { x ->
            repeat(32) { z ->
                val memIndex = (x shl 5) + z
                if (chunksPresent[memIndex]) {
                    chunks[memIndex] = WorldChunk(this, x, z).also { it.fromNbt(chunksNbt[dataIndex++]) }
                } else {
                    chunks[memIndex] = null
                }
            }
        }
    }

    fun save() {
        try {
            val tempFile = File.createTempFile("mckt-save", ".nbt")
            tempFile.outputStream().use { out ->
                writeNbtCompressed(toNbt(), out)
            }
            tempFile.toPath().moveTo(regionFile.toPath(), true)
        } catch (e: Exception) {
            LOGGER.error("Failed to save region $x, $z", e)
        }
    }

    override fun close() = save()
}