@file:UseSerializers(BitSetSerializer::class)

package io.github.gaming32.mckt

import io.github.gaming32.mckt.commands.CommandSender
import io.github.gaming32.mckt.objects.BitSetSerializer
import io.github.gaming32.mckt.objects.Identifier
import io.github.gaming32.mckt.packet.MinecraftOutputStream
import io.github.gaming32.mckt.worldgen.DefaultWorldGenerator
import kotlinx.coroutines.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import net.benwoodworth.knbt.*
import net.kyori.adventure.text.Component
import java.io.File
import java.io.FileNotFoundException
import java.util.BitSet
import kotlin.random.Random
import kotlin.reflect.typeOf
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.DurationUnit

private val LOGGER = getLogger()

val DEFAULT_REGISTRY_CODEC = buildNbtCompound("") {
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
}

object Blocks {
    val STONE = Identifier("stone")
    val DIRT = Identifier("dirt")
    val GRASS_BLOCK = Identifier("grass_block")
    val BEDROCK = Identifier("bedrock")
    val LAVA = Identifier("lava")
    val WOOD = Identifier("oak_log")
    val LEAVES = Identifier("oak_leaves")

    // These are used in memory only
    internal val BLOCK_NUM_TO_ID = arrayOf(STONE, DIRT, GRASS_BLOCK, BEDROCK, LAVA, WOOD, LEAVES)
    internal val BLOCK_ID_TO_NUM = BLOCK_NUM_TO_ID.withIndex().associate { it.value to (it.index + 1) }
    internal val BLOCK_NUM_TO_PALETTE = IntArray(BLOCK_NUM_TO_ID.size) { GLOBAL_PALETTE[BLOCK_NUM_TO_ID[it]] ?: 0 }
}

@Serializable
enum class WorldGenerator(val createGenerator: (seed: Long) -> suspend (chunk: WorldChunk) -> Unit) {
    @SerialName("flat") FLAT({
        { chunk ->
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
    @SerialName("normal") NORMAL({ seed -> DefaultWorldGenerator(seed).let { generator -> { chunk ->
        coroutineScope { launch(chunk.world.worldgenPool) {
            generator.generateChunk(chunk)
        } }
    } } })
}

class World(val server: MinecraftServer, val name: String) : AutoCloseable {
    val worldDir = File("worlds", name).apply { mkdirs() }
    val metaFile = File(worldDir, "meta.json")
    val playersDir = File(worldDir, "players").apply { mkdirs() }
    val regionsDir = File(worldDir, "regions").apply { mkdirs() }

    private val openRegions = mutableMapOf<Pair<Int, Int>, WorldRegion>()
    @OptIn(DelicateCoroutinesApi::class)
    internal val worldgenPool = if (Runtime.getRuntime().availableProcessors() == 1) {
        Dispatchers.Default
    } else {
        newFixedThreadPoolContext(Runtime.getRuntime().availableProcessors() - 1, "Worldgen")
    }

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

    fun getRegion(x: Int, z: Int) = openRegions.computeIfAbsent(x to z) { (x, z) -> WorldRegion(this, x, z) }

    fun getChunk(x: Int, z: Int): WorldChunk? =
        getRegion(x shr 5, z shr 5).getChunk(x and 31, z and 31)

    suspend fun getChunkOrElse(x: Int, z: Int, generate: suspend (WorldChunk) -> Unit = {}) =
        getRegion(x shr 5, z shr 5).getChunkOrElse(x and 31, z and 31, generate)

    suspend fun getChunkOrGenerate(x: Int, z: Int) = getChunkOrElse(x, z, worldGenerator)

    fun getBlock(x: Int, y: Int, z: Int) =
        getRegion(x shr 9, z shr 9).getBlock(x and 511, y, z and 511)

    suspend fun getBlockOrGenerate(x: Int, y: Int, z: Int) =
        getRegion(x shr 9, z shr 9).getBlockOrGenerate(x and 511, y, z and 511)

    fun setBlock(x: Int, y: Int, z: Int, id: Identifier?) =
        getRegion(x shr 9, z shr 9).setBlock(x and 511, y, z and 511, id)

    @OptIn(ExperimentalSerializationApi::class)
    fun save() {
        metaFile.outputStream().use { PRETTY_JSON.encodeToStream(meta, it) }
        openRegions.values.forEach(WorldRegion::save)
    }

    suspend fun saveAndLog(sender: CommandSender = server.serverCommandSender) {
        sender.replyBroadcast(Component.text("Saving world \"$name\""))
        val start = System.nanoTime()
        save()
        val duration = System.nanoTime() - start
        sender.replyBroadcast(Component.text(
            "Saved world \"$name\" in ${duration.nanoseconds.toDouble(DurationUnit.MILLISECONDS)}ms"
        ))
    }

    override fun close() {
        save()
        openRegions.clear()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun closeAndLog() {
        saveAndLog()
        if (worldgenPool is CloseableCoroutineDispatcher) {
            worldgenPool.close()
        }
        openRegions.clear()
    }
}

class WorldRegion(val world: World, val x: Int, val z: Int) : AutoCloseable {
    val regionFile = File(world.regionsDir, "region_${x}_${z}${world.meta.saveFormat.fileExtension}")

    @Serializable
    internal class RegionData(
        @SerialName("ChunksPresent") val chunksPresent: BitSet,
        @SerialName("Chunks")        val chunks: Array<WorldChunk.ChunkData>
    )

    private val chunks = arrayOfNulls<WorldChunk>(32 * 32)

    init {
        try {
            regionFile.inputStream().use { fromData(world.meta.saveFormat.decodeFromStream(it, typeOf<RegionData>())) }
        } catch (_: FileNotFoundException) {
        }
    }

    fun getChunk(x: Int, z: Int): WorldChunk? = chunks[(x shl 5) + z]

    suspend fun getChunkOrElse(x: Int, z: Int, generate: suspend (WorldChunk) -> Unit = {}): WorldChunk {
        var chunk = chunks[x * 32 + z]
        if (chunk == null) {
            chunk = WorldChunk(this, x, z)
            chunks[x * 32 + z] = chunk
            generate(chunk)
        }
        return chunk
    }

    suspend fun getChunkOrGenerate(x: Int, z: Int) = getChunkOrElse(x, z, world.worldGenerator)

    fun getBlock(x: Int, y: Int, z: Int) = chunks[(x shr 4 shl 5) + (z shr 4)]?.getBlock(x and 15, y, z and 15)

    suspend fun getBlockOrGenerate(x: Int, y: Int, z: Int) =
        getChunkOrGenerate(x shr 4, z shr 4).getBlock(x and 15, y, z and 15)

    fun setBlock(x: Int, y: Int, z: Int, id: Identifier?) =
        chunks[(x shr 4 shl 5) + (z shr 4)]?.setBlock(x and 15, y, z and 15, id)

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
        regionFile.outputStream().use { out ->
            world.meta.saveFormat.encodeToStream(toData(), out, typeOf<RegionData>())
        }
    }

    override fun close() = save()
}

class WorldChunk(val region: WorldRegion, val xInRegion: Int, val zInRegion: Int) {
    val world get() = region.world
    val x get() = (region.x shl 5) + xInRegion
    val z get() = (region.z shl 5) + zInRegion

    @Serializable
    internal class ChunkData(
        @SerialName("SectionsPresent") val sectionsPresent: BitSet,
        @SerialName("Sections")        val sections: Array<ChunkSection.SectionData>,
        @SerialName("Heightmap")       val heightmap: SimpleBitStorage
    )

    private val sections = arrayOfNulls<ChunkSection>(254)
    internal val heightmap = SimpleBitStorage(12, 16 * 16)

    fun getSection(y: Int): ChunkSection? = sections[y + 127]

    fun getBlock(x: Int, y: Int, z: Int): Identifier? {
        if (y < -2064 || y > 2063) return null
        val section = sections[(y shr 4) + 127] ?: return null
        return section.getBlock(x, y and 15, z)
    }

    fun setBlock(x: Int, y: Int, z: Int, id: Identifier?) {
        if (y < -2064 || y > 2063) return
        synchronized(this) {
            var section = sections[(y shr 4) + 127]
            if (section == null) {
                if (id == null) return
                section = ChunkSection(this, y shr 4)
                sections[(y shr 4) + 127] = section
            }
            section.setBlock(x, y and 15, z, id)
        }
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
        ChunkData(sectionsPresent, sectionsData.toTypedArray(), heightmap)
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
        require(input.heightmap.bits == heightmap.bits)
        require(input.heightmap.size == heightmap.size)
        input.heightmap.data.copyInto(heightmap.data)
    }

    fun networkEncode(out: MinecraftOutputStream) {
        for (section in sections) {
            if (section == null) {
                out.writeShort(0) // Block count
                out.writeByte(0) // Blocks: Bits per entry
                out.writeVarInt(0) // Blocks: Air ID
                out.writeVarInt(0) // Blocks: Data size
            } else {
                out.writeShort(section.blockCount)
                out.writeByte(4) // Blocks: Bits per entry
                out.writeVarInt(Blocks.BLOCK_NUM_TO_PALETTE.size + 1)
                out.writeVarInt(0)
                Blocks.BLOCK_NUM_TO_PALETTE.forEach { out.writeVarInt(it) }
                out.writeVarInt(256)
                for (i in 0 until 16 * 16 * 16 step 16) {
                    var value = 0L
                    for (j in i until i + 16) {
                        value = value shl 4 or section.data[j].toLong()
                    }
                    out.writeLong(value)
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
        @SerialName("Palette")    val palette: List<Identifier>,
        @SerialName("Blocks")     val blocks: ByteArray
    )

    internal val data = ByteArray(4096)

    var blockCount = 0
        private set

    private fun getBlockIndex(x: Int, y: Int, z: Int) = (y shl 8) + (z shl 4) + 15 - x

    fun getBlock(x: Int, y: Int, z: Int): Identifier? {
        val id = data[getBlockIndex(x, y, z)]
        if (id == 0.toByte()) return null
        return Blocks.BLOCK_NUM_TO_ID[id.toInt() - 1]
    }

    internal fun setBlock(x: Int, y: Int, z: Int, id: Identifier?) {
        val index = getBlockIndex(x, y, z)
        val old = data[index]
        if (id == null) {
            data[index] = 0
            if (old != 0.toByte()) blockCount--
            return
        }
        data[index] = Blocks.BLOCK_ID_TO_NUM[id]?.toByte()
            ?: throw IllegalArgumentException("Unknown block $id")
        if (old == 0.toByte()) blockCount++
    }

    internal fun toData(): SectionData {
        val palette = mutableSetOf<Identifier>()
        for (block in data) {
            if (block == 0.toByte()) continue // Air
            palette.add(Blocks.BLOCK_NUM_TO_ID[block - 1])
        }
        if (palette.size > 255) {
            throw IllegalArgumentException("Section too complex")
        }
        val paletteList = palette.toList()
        if (paletteList.size == 1 && blockCount == 4096) {
            return SectionData(blockCount, paletteList, ByteArray(0))
        }
        val localIds = ByteArray(Blocks.BLOCK_NUM_TO_ID.size) {
            (paletteList.indexOf(Blocks.BLOCK_NUM_TO_ID[it]) + 1).toByte()
        }
        val packedData = ByteArray(4096)
        repeat(16 * 16 * 16) { i ->
            val block = data[i].toUByte().toInt()
            packedData[i] = if (block == 0) 0 else localIds[block - 1]
        }
        return SectionData(blockCount, paletteList, packedData)
    }

    internal fun fromData(input: SectionData) {
        blockCount = input.blockCount
        val palette = ByteArray(input.palette.size) {
            Blocks.BLOCK_ID_TO_NUM[input.palette[it]]?.toByte() ?:
                throw IllegalArgumentException("Unknown block ID ${input.palette[it]}")
        }
        if (input.blocks.isEmpty()) {
            return data.fill(palette[0])
        }
        repeat(16 * 16 * 16) { i ->
            val block = input.blocks[i].toUByte().toInt()
            data[i] = if (block == 0) 0 else palette[block - 1]
        }
    }
}

@Serializable
class WorldMeta() {
    var time = 0L
    var seed = 0L
    var worldGenerator = WorldGenerator.NORMAL
    var saveFormat = SaveFormat.NBT

    constructor(config: ServerConfig) : this() {
        seed = config.seed ?: Random.nextLong()
        worldGenerator = config.defaultWorldGenerator
        saveFormat = config.defaultSaveFormat
    }
}

@Serializable
class PlayerData {
    var x = 0.0
    var y = 5.0
    var z = 0.0
    var yaw = 0f
    var pitch = 0f
    var onGround = false
    var flying = false
    var operatorLevel = 0
    var gamemode = Gamemode.CREATIVE
    var flags: Int = 0

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
