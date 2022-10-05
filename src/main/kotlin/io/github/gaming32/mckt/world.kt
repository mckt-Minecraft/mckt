@file:UseSerializers(BitSetSerializer::class)

package io.github.gaming32.mckt

import io.github.gaming32.mckt.GlobalPalette.DEFAULT_BLOCKSTATES
import io.github.gaming32.mckt.commands.CommandSource
import io.github.gaming32.mckt.data.writeByte
import io.github.gaming32.mckt.data.writeShort
import io.github.gaming32.mckt.data.writeVarInt
import io.github.gaming32.mckt.objects.*
import io.github.gaming32.mckt.packet.play.s2c.WorldEventPacket
import io.github.gaming32.mckt.util.IntIntPair2ObjectMap
import io.github.gaming32.mckt.util.PalettedStorage
import io.github.gaming32.mckt.util.SimpleBitStorage
import io.github.gaming32.mckt.worldgen.DefaultWorldGenerator
import io.ktor.util.collections.*
import kotlinx.coroutines.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import net.benwoodworth.knbt.*
import net.kyori.adventure.text.Component
import org.intellij.lang.annotations.MagicConstant
import java.io.File
import java.io.FileNotFoundException
import java.io.OutputStream
import java.util.*
import kotlin.io.path.moveTo
import kotlin.random.Random
import kotlin.reflect.typeOf
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.DurationUnit

private val LOGGER = getLogger()

val DEFAULT_REGISTRY_CODEC = buildNbtCompound {
    putNbtCompound("minecraft:dimension_type") {
        put("type", "minecraft:dimension_type")
        putNbtList("value") {
            addNbtCompound {
                put("name", "minecraft:overworld")
                put("id", 0)
                putNbtCompound("element") {
                    put("name", "minecraft:overworld")
                    put("ultrawarm", false)
                    put("natural", true)
                    put("coordinate_scale", 1f)
                    put("has_skylight", true)
                    put("has_ceiling", false)
                    put("ambient_light", 1f)
                    put("monster_spawn_light_level", 0)
                    put("monster_spawn_block_light_limit", 0)
                    put("piglin_safe", false)
                    put("bed_works", true)
                    put("respawn_anchor_works", false)
                    put("has_raids", true)
                    put("logical_height", 4064)
                    put("min_y", -2032)
                    put("height", 4064)
                    put("infiniburn", "#minecraft:infiniburn_overworld")
                    put("effects", "minecraft:overworld")
                }
            }
        }
    }
    putNbtCompound("minecraft:worldgen/biome") {
        put("type", "minecraft:worldgen/biome")
        putNbtList("value") {
            addNbtCompound {
                put("name", "minecraft:plains")
                put("id", 0)
                putNbtCompound("element") {
                    put("name", "minecraft:plains")
                    put("precipitation", "rain")
                    put("depth", 0.125f)
                    put("temperature", 0.8f)
                    put("scale", 0.05f)
                    put("downfall", 0.4f)
                    put("category", "plains")
                    putNbtCompound("effects") {
                        put("sky_color", 0x78A7FFL)
                        put("water_fog_color", 0x050533L)
                        put("fog_color", 0xC0D8FFL)
                        put("water_color", 0x3F76E4L)
                    }
                    putNbtCompound("mood_sound") {
                        put("tick_delay", 6000)
                        put("offset", 2f)
                        put("sound", "minecraft:ambient_cave")
                        put("block_search_extent", 8)
                    }
                }
            }
        }
    }
    putNbtCompound("minecraft:chat_type") {
        put("type", "minecraft:chat_type")
        putNbtList("value") {
            addNbtCompound {
                put("name", "minecraft:chat")
                put("id", 0)
                putNbtCompound("element") {
                    putNbtCompound("chat") {
                        put("translation_key", "chat.type.text")
                        putNbtList("parameters") {
                            this.add("sender")
                            this.add("content")
                        }
                    }
                    putNbtCompound("narration") {
                        put("translation_key", "chat.type.text.narrate")
                        putNbtList("parameters") {
                            this.add("sender")
                            this.add("content")
                        }
                    }
                }
            }
            addNbtCompound {
                put("name", "minecraft:say_command")
                put("id", 1)
                putNbtCompound("element") {
                    putNbtCompound("chat") {
                        put("translation_key", "chat.type.announcement")
                        putNbtList("parameters") {
                            this.add("sender")
                            this.add("content")
                        }
                    }
                    putNbtCompound("narration") {
                        put("translation_key", "chat.type.text.narrate")
                        putNbtList("parameters") {
                            this.add("sender")
                            this.add("content")
                        }
                    }
                }
            }
            addNbtCompound {
                put("name", "minecraft:msg_command_incoming")
                put("id", 2)
                putNbtCompound("element") {
                    putNbtCompound("chat") {
                        put("translation_key", "commands.message.display.incoming")
                        putNbtCompound("style") {
                            put("color", "gray")
                            put("italic", true)
                        }
                        putNbtList("parameters") {
                            this.add("sender")
                            this.add("content")
                        }
                    }
                    putNbtCompound("narration") {
                        put("translation_key", "chat.type.text.narrate")
                        putNbtList("parameters") {
                            this.add("sender")
                            this.add("content")
                        }
                    }
                }
            }
            addNbtCompound {
                put("name", "minecraft:msg_command_outgoing")
                put("id", 3)
                putNbtCompound("element") {
                    putNbtCompound("chat") {
                        put("translation_key", "commands.message.display.outgoing")
                        putNbtCompound("style") {
                            put("color", "gray")
                            put("italic", true)
                        }
                        putNbtList("parameters") {
                            this.add("sender")
                            this.add("content")
                        }
                    }
                    putNbtCompound("narration") {
                        put("translation_key", "chat.type.text.narrate")
                        putNbtList("parameters") {
                            this.add("sender")
                            this.add("content")
                        }
                    }
                }
            }
            addNbtCompound {
                put("name", "minecraft:team_msg_command_incoming")
                put("id", 4)
                putNbtCompound("element") {
                    putNbtCompound("chat") {
                        put("translation_key", "chat.type.team.text")
                        putNbtList("parameters") {
                            this.add("target")
                            this.add("sender")
                            this.add("content")
                        }
                    }
                    putNbtCompound("narration") {
                        put("translation_key", "chat.type.text.narrate")
                        putNbtList("parameters") {
                            this.add("sender")
                            this.add("content")
                        }
                    }
                }
            }
            addNbtCompound {
                put("name", "minecraft:team_msg_command_outgoing")
                put("id", 5)
                putNbtCompound("element") {
                    putNbtCompound("chat") {
                        put("translation_key", "chat.type.team.sent")
                        putNbtList("parameters") {
                            this.add("target")
                            this.add("sender")
                            this.add("content")
                        }
                    }
                    putNbtCompound("narration") {
                        put("translation_key", "chat.type.text.narrate")
                        putNbtList("parameters") {
                            this.add("sender")
                            this.add("content")
                        }
                    }
                }
            }
            addNbtCompound {
                put("name", "minecraft:emote_command")
                put("id", 6)
                putNbtCompound("element") {
                    putNbtCompound("chat") {
                        put("translation_key", "chat.type.emote")
                        putNbtList("parameters") {
                            this.add("sender")
                            this.add("content")
                        }
                    }
                    putNbtCompound("narration") {
                        put("translation_key", "chat.type.emote")
                        putNbtList("parameters") {
                            this.add("sender")
                            this.add("content")
                        }
                    }
                }
            }
        }
    }
}

object Blocks {
    private fun getBlock(id: String) =
        DEFAULT_BLOCKSTATES[Identifier.parse(id)] ?: throw Error("Standard block $id not found")

    val AIR = getBlock("air")
    val STONE = getBlock("stone")
    val DIRT = getBlock("dirt")
    val GRASS_BLOCK = getBlock("grass_block")
    val BEDROCK = getBlock("bedrock")
    val LAVA = getBlock("lava")
    val OAK_LOG = getBlock("oak_log")
    val OAK_LEAVES = getBlock("oak_leaves")
    val DIORITE = getBlock("diorite")
    val ANDESITE = getBlock("andesite")
    val GRANITE = getBlock("granite")
    val FIRE = getBlock("fire")
    val BIRCH_LOG = getBlock("birch_log")
    val BIRCH_LEAVES = getBlock("birch_leaves")
}

object Materials {
    private fun getMaterial(name: String) =
        BLOCK_MATERIALS[name] ?: throw Error("Standard block $name not found")

    val METAL = getMaterial("metal")
}

data class GeneratorArgs(
    val chunk: BlockAccess,
    val world: World,
    val chunkX: Int,
    val chunkZ: Int
) {
    constructor(chunk: WorldChunk) : this(chunk, chunk.world, chunk.x, chunk.z)
}

@Serializable
enum class WorldGenerator(val createGenerator: (seed: Long) -> suspend (GeneratorArgs) -> Unit) {
    @SerialName("flat") FLAT({
        { args ->
            val chunk = args.chunk
            repeat(16) { x ->
                repeat(16) { z ->
                    chunk.setBlock(x, 0, z, Blocks.BEDROCK)
                    chunk.setBlock(x, 1, z, Blocks.DIRT)
                    chunk.setBlock(x, 2, z, Blocks.DIRT)
                    chunk.setBlock(x, 3, z, Blocks.GRASS_BLOCK)
                }
            }
        }
    }),
    @SerialName("normal") NORMAL({ seed -> DefaultWorldGenerator(seed).let { generator -> { args ->
        coroutineScope { launch(args.world.worldgenPool) {
            generator.generateChunk(args.chunk, args.chunkX, args.chunkZ)
        } }
    } } })
}

interface SuspendingBlockAccess {
    suspend fun getBlock(x: Int, y: Int, z: Int): BlockState = getBlock(BlockPosition(x, y, z))

    suspend fun getBlock(pos: BlockPosition): BlockState = getBlock(pos.x, pos.y, pos.z)

    suspend fun setBlock(x: Int, y: Int, z: Int, block: BlockState): Boolean = setBlock(BlockPosition(x, y, z), block)

    suspend fun setBlock(pos: BlockPosition, block: BlockState): Boolean =
        setBlock(pos.x, pos.y, pos.z, block)
}

interface BlockAccess : SuspendingBlockAccess {
    override suspend fun getBlock(x: Int, y: Int, z: Int) = getBlockImmediate(x, y, z)
    override suspend fun getBlock(pos: BlockPosition) = getBlockImmediate(pos)
    override suspend fun setBlock(x: Int, y: Int, z: Int, block: BlockState) = setBlockImmediate(x, y, z, block)
    override suspend fun setBlock(pos: BlockPosition, block: BlockState) = setBlockImmediate(pos, block)

    fun getBlockImmediate(x: Int, y: Int, z: Int): BlockState = getBlockImmediate(BlockPosition(x, y, z))

    fun getBlockImmediate(pos: BlockPosition): BlockState =
        getBlockImmediate(pos.x, pos.y, pos.z)

    fun setBlockImmediate(x: Int, y: Int, z: Int, block: BlockState): Boolean =
        setBlockImmediate(BlockPosition(x, y, z), block)

    fun setBlockImmediate(pos: BlockPosition, block: BlockState): Boolean =
        setBlockImmediate(pos.x, pos.y, pos.z, block)
}

object SetBlockFlags {
    const val PERFORM_NEIGHBOR_UPDATE = 0x01
    const val NO_BLOCK_UPDATE = 0x10

    const val DEFAULT_FLAGS = 0
}

class World(val server: MinecraftServer, val name: String) : BlockAccess {
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
    }

    val worldGenerator = meta.worldGenerator.createGenerator(meta.seed)

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

    suspend fun getChunkOrElse(x: Int, z: Int, generate: suspend (GeneratorArgs) -> Unit = {}) =
        getRegion(x shr 5, z shr 5).getChunkOrElse(x and 31, z and 31, generate)

    suspend fun getChunkOrGenerate(x: Int, z: Int) = getChunkOrElse(x, z, worldGenerator)

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
                    direction.opposite, block, this, pos, neighborPos
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

    suspend fun findSpawnPoint(): BlockPosition {
        if (meta.spawnPos != BlockPosition.ZERO) {
            return meta.spawnPos
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
        metaFile.outputStream().use { PRETTY_JSON.encodeToStream(meta, it) }
        openRegions.values.map { region ->
            launch(saveLoadPool) { region.save() }
        }.joinAll()
        val duration = System.nanoTime() - start
        commandSource.replyBroadcast(Component.translatable(
            "commands.save.success",
            Component.text(name),
            Component.text(duration.nanoseconds.toDouble(DurationUnit.MILLISECONDS))
        ))
        isSaving = false
    }

    suspend fun closeAndLog() {
        saveAndLog()
        openRegions.clear()
    }
}

class WorldRegion(val world: World, val x: Int, val z: Int) : AutoCloseable, BlockAccess {
    val regionFile = File(world.regionsDir, "region_${x}_${z}${world.meta.saveFormat.fileExtension}")

    @Serializable
    internal class RegionData(
        @SerialName("ChunksPresent") val chunksPresent: BitSet,
        @SerialName("Chunks")        val chunks: Array<WorldChunk.ChunkData>
    )

    internal var loadJob: Job? = null

    private val chunks = arrayOfNulls<WorldChunk>(32 * 32)

    fun load() {
        try {
            regionFile.inputStream().use { fromData(world.meta.saveFormat.decodeFromStream(it, typeOf<RegionData>())) }
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

    suspend fun getChunkOrGenerate(x: Int, z: Int) = getChunkOrElse(x, z, world.worldGenerator)

    override fun getBlockImmediate(x: Int, y: Int, z: Int) =
        chunks[(x shr 4 shl 5) + (z shr 4)]?.getBlockImmediate(x and 15, y, z and 15) ?: Blocks.AIR

    suspend fun getBlockOrGenerate(x: Int, y: Int, z: Int) =
        getChunkOrGenerate(x shr 4, z shr 4).getBlockImmediate(x and 15, y, z and 15)

    suspend fun getBlockOrGenerate(pos: BlockPosition) = getBlockOrGenerate(pos.x, pos.y, pos.z)

    override fun setBlockImmediate(x: Int, y: Int, z: Int, block: BlockState) =
        chunks[(x shr 4 shl 5) + (z shr 4)]?.setBlockImmediate(x and 15, y, z and 15, block) ?: false

    internal fun toData(): RegionData {
        val chunksPresent = BitSet(chunks.size)
        val chunksData = mutableListOf<WorldChunk.ChunkData>()
        chunks.toList().forEachIndexed { i, chunk ->
            if (chunk != null) {
                chunksPresent.set(i)
                chunksData.add(chunk.toData())
            }
        }
        return RegionData(chunksPresent, chunksData.toTypedArray())
    }

    internal fun fromData(input: RegionData) {
        var dataIndex = 0
        repeat(32) { x ->
            repeat(32) { z ->
                val memIndex = (x shl 5) + z
                if (input.chunksPresent[memIndex]) {
                    chunks[memIndex] = WorldChunk(this, x, z).also { it.fromData(input.chunks[dataIndex++]) }
                } else {
                    chunks[memIndex] = null
                }
            }
        }
    }

    fun save() {
        val tempFile = File.createTempFile("mckt-save", ".nbt")
        tempFile.outputStream().use { out ->
            world.meta.saveFormat.encodeToStream(toData(), out, typeOf<RegionData>())
        }
        tempFile.toPath().moveTo(regionFile.toPath(), true)
    }

    override fun close() = save()
}

class WorldChunk(val region: WorldRegion, val xInRegion: Int, val zInRegion: Int) : BlockAccess {
    val world get() = region.world
    val x get() = (region.x shl 5) + xInRegion
    val z get() = (region.z shl 5) + zInRegion

    @Serializable
    internal class ChunkData(
        @SerialName("SectionsPresent") val sectionsPresent: BitSet,
        @SerialName("Sections")        val sections: Array<ChunkSection.SectionData>
    )

    private val sections = arrayOfNulls<ChunkSection>(254)
    internal var ready = false

    fun getSection(y: Int): ChunkSection? = sections[y + 127]

    override fun getBlockImmediate(x: Int, y: Int, z: Int): BlockState {
        if (y < -2032 || y > 2031) return Blocks.AIR
        val section = sections[(y shr 4) + 127] ?: return Blocks.AIR
        return section.getBlock(x, y and 15, z)
    }

    override fun setBlockImmediate(x: Int, y: Int, z: Int, block: BlockState): Boolean {
        if (y < -2032 || y > 2031) return false
        synchronized(this) {
            var section = sections[(y shr 4) + 127]
            if (section == null) {
                if (block == Blocks.AIR) return true
                section = ChunkSection(this, y shr 4)
                sections[(y shr 4) + 127] = section
            }
            section.setBlock(x, y and 15, z, block)
            if (section.blockCount == 0) { // The section is now empty
                sections[(y shr 4) + 127] = null
            }
        }
        if (ready) {
            world.dirtyBlocks.add(BlockPosition((this.x shl 4) + x, y, (this.z shl 4) + z))
        }
        return true
    }

    internal fun toData() = synchronized(this) {
        val sectionsPresent = BitSet(sections.size)
        val sectionsData = mutableListOf<ChunkSection.SectionData>()
        sections.forEachIndexed { i, section ->
            if (section != null) {
                sectionsPresent.set(i)
                sectionsData.add(section.toData())
            }
        }
        ChunkData(sectionsPresent, sectionsData.toTypedArray())
    }

    internal fun fromData(input: ChunkData) {
        var dataIndex = 0
        repeat(254) { i ->
            if (input.sectionsPresent[i]) {
                sections[i] = ChunkSection(this, i - 127).also { it.fromData(input.sections[dataIndex++]) }
            } else {
                sections[i] = null
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

class ChunkSection(val chunk: WorldChunk, val y: Int) {
    val world get() = chunk.region.world
    val x get() = chunk.x
    val z get() = chunk.z
    val region get() = chunk.region
    val xInRegion get() = chunk.xInRegion
    val zInRegion get() = chunk.zInRegion

    @Serializable
    internal class SectionData(
        @SerialName("BlockCount") val blockCount: Int,
        @SerialName("Palette")    val palette: List<Map<String, String>>,
        @SerialName("Blocks")     val blocks: SimpleBitStorage
    )

    internal val data = PalettedStorage(4096, Blocks.AIR) { writeVarInt(it.globalId) }

    var blockCount = 0
        private set

    private fun getBlockIndex(x: Int, y: Int, z: Int) = (y shl 8) + (z shl 4) + x

    fun getBlock(x: Int, y: Int, z: Int) = data[getBlockIndex(x, y, z)]

    fun getBlock(pos: BlockPosition) = getBlock(pos.x, pos.y, pos.z)

    internal fun setBlock(x: Int, y: Int, z: Int, block: BlockState) {
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
    }

    internal fun toData(): SectionData {
        data.compact()
        return SectionData(
            blockCount,
            data.paletteItems
                .map { it!!.toMap() }
                .toList(),
            data.storage
        )
    }

    internal fun fromData(input: SectionData) {
        blockCount = input.blockCount
        data.setPaletteItems(input.palette.map { BlockState.fromMap(it) })
        data.storage = input.blocks
    }
}

@Serializable
class WorldMeta() {
    var time = 0L
    var seed = 0L
    var spawnPos = BlockPosition.ZERO
    var worldGenerator = WorldGenerator.NORMAL
    var saveFormat = SaveFormat.NBT
    var autosave = true

    constructor(config: ServerConfig) : this() {
        seed = config.seed ?: Random.nextLong()
        worldGenerator = config.defaultWorldGenerator
        saveFormat = config.defaultSaveFormat
    }
}

@Serializable
class PlayerData(
    var x: Double = 0.0,
    var y: Double = 0.0,
    var z: Double = 0.0
) {
    var yaw = 0f
    var pitch = 0f
    var onGround = false
    var flying = false
    var operatorLevel = 0
    var gamemode = Gamemode.CREATIVE
    val inventory = Array(46) { ItemStack.EMPTY }
    var selectedHotbarSlot = 0
        set(value) {
            require(value in 0..9) { "Hotbar slot not in range 0..9" }
            field = value
        }

    var selectedInventorySlot
        get() = selectedHotbarSlot + 36
        set(value) {
            selectedHotbarSlot = value - 36
        }

    var mainHand
        get() = inventory[selectedInventorySlot]
        set(value) {
            inventory[selectedInventorySlot] = value
        }
    var offhand
        get() = inventory[45]
        set(value) {
            inventory[45] = value
        }

    fun getHeldItem(hand: Hand) = if (hand == Hand.MAINHAND) mainHand else offhand
    fun setHeldItem(hand: Hand, stack: ItemStack) {
        if (hand == Hand.MAINHAND) {
            mainHand = stack
        } else {
            offhand = stack
        }
    }

    var flags: Int = 0
    var pose: EntityPose = EntityPose.STANDING

    var isSneaking: Boolean
        get() = (flags and EntityFlags.SNEAKING) != 0
        set(value) {
            flags = if (value) {
                flags or EntityFlags.SNEAKING
            } else {
                flags and EntityFlags.SNEAKING.inv()
            }
        }

    var isSprinting: Boolean
        get() = (flags and EntityFlags.SPRINTING) != 0
        set(value) {
            flags = if (value) {
                flags or EntityFlags.SPRINTING
            } else {
                flags and EntityFlags.SPRINTING.inv()
            }
        }

    var isFallFlying: Boolean
        get() = (flags and EntityFlags.FALL_FLYING) != 0
        set(value) {
            flags = if (value) {
                flags or EntityFlags.FALL_FLYING
            } else {
                flags and EntityFlags.FALL_FLYING.inv()
            }
        }

    fun getEquipment(): Map<EquipmentSlot, ItemStack> {
        val result = enumMapOf<EquipmentSlot, ItemStack>()
        for (slot in EquipmentSlot.values()) {
            val rawSlot = if (slot.rawSlot == -1) selectedInventorySlot else slot.rawSlot
            if (inventory[rawSlot].isNotEmpty()) {
                result[slot] = inventory[rawSlot]
            }
        }
        return result
    }
}

data class BlocksView(val inner: BlockAccess, val offsetX: Int, val offsetY: Int, val offsetZ: Int) : BlockAccess {
    constructor(inner: BlockAccess, offset: BlockPosition) : this(inner, offset.x, offset.y, offset.z)

    override suspend fun getBlock(x: Int, y: Int, z: Int) = inner.getBlock(x + offsetX, y + offsetY, z + offsetZ)

    override suspend fun setBlock(x: Int, y: Int, z: Int, block: BlockState) =
        inner.setBlock(x + offsetX, y + offsetY, z + offsetZ, block)

    override fun getBlockImmediate(x: Int, y: Int, z: Int) =
        inner.getBlockImmediate(x + offsetX, y + offsetY, z + offsetZ)

    override fun setBlockImmediate(x: Int, y: Int, z: Int, block: BlockState) =
        inner.setBlockImmediate(x + offsetX, y + offsetY, z + offsetZ, block)
}

object EntityFlags {
    const val BURNING = 0x01
    const val SNEAKING = 0x02
    const val SPRINTING = 0x08
    const val SWIMMING = 0x10
    const val INVISIBLE = 0x20
    const val GLOWING = 0x40
    const val FALL_FLYING = 0x80
}

val BlockPosition.isInBuildLimit get() = y in -2032 until 2032 && isValidHorizontally
val BlockPosition.isValidForWorld get() = y in -20000000 until 20000000 && isValidHorizontally
private val BlockPosition.isValidHorizontally inline get() =
    x in -30000000 until 30000000 && z in -30000000 until 30000000
