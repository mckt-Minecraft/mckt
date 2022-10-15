package io.github.gaming32.mckt.world

import io.github.gaming32.mckt.MinecraftServer
import io.github.gaming32.mckt.PRETTY_JSON
import io.github.gaming32.mckt.blocks.entities.BlockEntity
import io.github.gaming32.mckt.commands.CommandSource
import io.github.gaming32.mckt.getLogger
import io.github.gaming32.mckt.objects.BlockPosition
import io.github.gaming32.mckt.objects.BlockState
import io.github.gaming32.mckt.objects.Direction
import io.github.gaming32.mckt.objects.Identifier
import io.github.gaming32.mckt.packet.play.s2c.WorldEventPacket
import io.github.gaming32.mckt.util.IntIntPair2ObjectMap
import io.github.gaming32.mckt.world.gen.GeneratorArgs
import io.github.gaming32.mckt.world.gen.WorldGenerators
import io.ktor.util.collections.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import net.kyori.adventure.text.Component
import org.intellij.lang.annotations.MagicConstant
import java.io.File
import java.io.FileNotFoundException
import kotlin.random.Random
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.DurationUnit

class World(val server: MinecraftServer, val name: String) : BlockAccess {
    companion object {
        private val LOGGER = getLogger()
    }

    val worldDir = File("worlds", name).apply { mkdirs() }
    val metaFile = File(worldDir, "meta.json")
    val playersDir = File(worldDir, "players").apply { mkdirs() }
    val regionsDir = File(worldDir, "regions").apply { mkdirs() }

    internal val openRegions = IntIntPair2ObjectMap<WorldRegion>()
    internal var dirtyBlocks = ConcurrentSet<BlockPosition>()
    var isSaving = false
        private set

    internal val worldgenPool = server.threadPoolContext
    private val saveLoadPool = server.threadPoolContext
    internal val networkSerializationPool = server.threadPoolContext

    @OptIn(ExperimentalSerializationApi::class)
    val meta = try {
        metaFile.inputStream().use { PRETTY_JSON.decodeFromStream(it) }
    } catch (e: Exception) {
        if (e !is FileNotFoundException) {
            LOGGER.warn("Couldn't read world meta, creating anew", e)
        }
        WorldMeta(server.config)
    }.also { meta ->
        if (meta.worldGenerator == Identifier("normal")) {
            meta.worldGenerator = Identifier("mckt", "default")
        }
    }

    val worldGenerator = WorldGenerators.getGenerator(meta.worldGenerator).let { type ->
            type.createGenerator(meta.seed, type.deserializeConfig(meta.generatorConfig))
        }

    suspend fun getRegion(x: Int, z: Int) = coroutineScope {
        var region = openRegions[x, z]
        if (region == null) {
            region = WorldRegion(this@World, x, z)
            openRegions[x, z] = region
            region.loadJob = launch(saveLoadPool) { region.load() }
        }
        region.loadJob?.join()
        region.loadJob = null
        region
    }

    fun getLoadedRegion(x: Int, z: Int) =
        openRegions[x, z] ?: throw IllegalStateException("Failed to get unloaded region $x $z")

    suspend fun getChunk(x: Int, z: Int): WorldChunk? =
        getRegion(x shr 5, z shr 5).getChunk(x and 31, z and 31)

    fun getLoadedChunk(x: Int, z: Int) = openRegions[x shr 5, z shr 5]?.getChunk(x and 31, z and 31)

    suspend fun getChunkOrElse(x: Int, z: Int, generate: suspend (GeneratorArgs) -> Unit = {}) =
        getRegion(x shr 5, z shr 5).getChunkOrElse(x and 31, z and 31, generate)

    suspend fun getChunkOrGenerate(x: Int, z: Int) = getChunkOrElse(x, z, worldGenerator::generateChunkThreaded)

    override suspend fun getBlock(x: Int, y: Int, z: Int) =
        getRegion(x shr 9, z shr 9).getBlockImmediate(x and 511, y, z and 511)

    suspend fun getBlockOrGenerate(x: Int, y: Int, z: Int) =
        getRegion(x shr 9, z shr 9).getBlockOrGenerate(x and 511, y, z and 511)

    suspend fun getBlockOrGenerate(pos: BlockPosition) = getBlockOrGenerate(pos.x, pos.y, pos.z)

    fun getBlockImmediateOrNull(x: Int, y: Int, z: Int) = openRegions[x shr 9, z shr 9]?.getBlockImmediate(x and 511, y, z and 511)

    fun getBlockImmediateOrNull(pos: BlockPosition) = getBlockImmediateOrNull(pos.x, pos.y, pos.z)

    override fun getBlockImmediate(x: Int, y: Int, z: Int) = getBlockImmediateOrNull(x, y, z) ?: Blocks.AIR

    suspend fun setBlock(
        pos: BlockPosition, block: BlockState,
        @MagicConstant(valuesFromClass = SetBlockFlags::class) flags: Int,
        maxUpdateDepth: Int = 512
    ): Boolean {
        if (maxUpdateDepth <= 0) return false
        val region = getRegion(pos.x shr 9, pos.z shr 9)
        val oldState = region.getBlockImmediate(pos.x and 511, pos.y, pos.z and 511)
        if (!region.setBlockImmediate(pos.x and 511, pos.y, pos.z and 511, block)) {
            return false
        }
//        if ((flags and SetBlockFlags.PERFORM_BLOCK_UPDATE) != 0) {
//        }
        if ((flags and SetBlockFlags.NO_BLOCK_UPDATE) == 0) {
            val updateFlags = flags and SetBlockFlags.PERFORM_NEIGHBOR_UPDATE.inv()
            oldState.prepare(this, pos, updateFlags, maxUpdateDepth - 1)
            Direction.values().forEach { direction ->
                val neighborPos = pos + direction.vector
                val oldNeighborState = getBlock(neighborPos)
                val newState = oldNeighborState.getStateForNeighborUpdate(
                    direction.opposite, block, this, neighborPos, pos
                )
                if (newState != oldNeighborState) {
                    if (newState == Blocks.AIR) {
                        breakBlock(neighborPos, maxUpdateDepth)
                    } else {
                        setBlock(neighborPos, newState, flags, maxUpdateDepth - 1)
                    }
                }
            }
            block.prepare(this, pos, updateFlags, maxUpdateDepth)
        }
        return true
    }

    suspend fun setBlock(
        x: Int, y: Int, z: Int, block: BlockState,
        @MagicConstant(valuesFromClass = SetBlockFlags::class) flags: Int
    ) = setBlock(BlockPosition(x, y, z), block, flags)

    suspend fun breakBlock(pos: BlockPosition, maxUpdateDepth: Int = 512) {
        val block = getBlock(pos)
        if (block == Blocks.AIR) return
//        if (block.getHandler(server) !is FireBlockHandler) {
            server.broadcast(WorldEventPacket(WorldEventPacket.BREAK_BLOCK, pos, block.globalId))
//        }
        setBlock(pos, Blocks.AIR, SetBlockFlags.PERFORM_NEIGHBOR_UPDATE, maxUpdateDepth)
    }

    override suspend fun setBlock(pos: BlockPosition, block: BlockState) =
        setBlock(pos, block, SetBlockFlags.DEFAULT_FLAGS)

    fun setBlockImmediate(
        x: Int, y: Int, z: Int, block: BlockState,
        @MagicConstant(valuesFromClass = SetBlockFlags::class) flags: Int
    ): Boolean {
        if (openRegions[x shr 9, z shr 9]?.setBlockImmediate(x and 511, y, z and 511, block) != true) return false
        return true
    }

    fun setBlockImmediate(
        pos: BlockPosition, block: BlockState,
        @MagicConstant(valuesFromClass = SetBlockFlags::class) flags: Int
    ) = setBlockImmediate(pos.x, pos.y, pos.z, block, flags)

    override fun setBlockImmediate(x: Int, y: Int, z: Int, block: BlockState) =
        setBlockImmediate(x, y, z, block, SetBlockFlags.DEFAULT_FLAGS)

    fun addBlockEntity(entity: BlockEntity<*>) {
        getLoadedChunk(entity.pos.x shr 4, entity.pos.z shr 4)?.addBlockEntity(entity)
    }

    fun getBlockEntity(pos: BlockPosition) =
        getLoadedChunk(pos.x shr 4, pos.z shr 4)?.getBlockEntity(BlockPosition(pos.x and 15, pos.y, pos.z and 15))

    fun removeBlockEntity(pos: BlockPosition) {
        getLoadedChunk(pos.x shr 4, pos.z shr 4)?.removeBlockEntity(BlockPosition(pos.x and 15, pos.y, pos.z and 15))
    }

    suspend fun getSpawnPoint(): BlockPosition {
        if (meta.spawnPos != null) {
            return meta.spawnPos!!
        }
        val rand = Random(meta.seed)
        val x = rand.nextInt(-256, 257)
        val z = rand.nextInt(-256, 257)
        var y = 0
        while (true) {
            val check = BlockPosition(x, y, z)
            if (getBlockOrGenerate(check) == Blocks.AIR) {
                if (getBlockOrGenerate(check.down()) != Blocks.AIR) {
                    if (getBlockOrGenerate(check.up()) == Blocks.AIR) {
                        meta.spawnPos = check
                        return check
                    }
                    y++
                    continue
                }
                y--
                continue
            }
            y++
        }
    }

    fun isBlockLoaded(pos: BlockPosition) = pos.isInBuildLimit && isRegionLoaded(pos.x shr 9, pos.z shr 9)

    fun isChunkLoaded(chunkX: Int, chunkZ: Int) = isRegionLoaded(chunkX shr 5, chunkZ shr 5)

    fun isRegionLoaded(regionX: Int, regionZ: Int) = openRegions.contains(regionX, regionZ)

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun saveAndLog(commandSource: CommandSource = server.serverCommandSender) = coroutineScope {
        while (isSaving) yield()
        isSaving = true
        commandSource.replyBroadcast(Component.translatable("commands.save.saving", Component.text(name)))
        val start = System.nanoTime()
        try {
            metaFile.outputStream().use { PRETTY_JSON.encodeToStream(meta, it) }
        } catch (e: Exception) {
            LOGGER.error("Failed to save world metadata", e)
        }
        openRegions.values.map { region ->
            launch(saveLoadPool) { region.save() }
        }.joinAll()
        val duration = System.nanoTime() - start
        commandSource.replyBroadcast(
            Component.translatable(
                "commands.save.success",
                Component.text(name),
                Component.text(duration.nanoseconds.toDouble(DurationUnit.MILLISECONDS))
            )
        )
        isSaving = false
    }

    suspend fun closeAndLog() {
        saveAndLog()
        openRegions.clear()
    }
}