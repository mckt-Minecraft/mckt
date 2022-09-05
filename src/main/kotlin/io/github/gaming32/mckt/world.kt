@file:UseSerializers(BitSetSerializer::class)

package io.github.gaming32.mckt

import io.github.gaming32.mckt.objects.BitSetSerializer
import io.github.gaming32.mckt.objects.Identifier
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import net.benwoodworth.knbt.*
import java.io.File
import java.io.FileNotFoundException
import java.util.BitSet
import kotlin.reflect.typeOf

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

    // These are used in memory only
    internal val BLOCK_NUM_TO_ID = listOf(STONE, DIRT, GRASS_BLOCK, BEDROCK, LAVA)
    internal val BLOCK_ID_TO_NUM = BLOCK_NUM_TO_ID.withIndex().associate { it.value to (it.index + 1) }
}

@Serializable
enum class WorldGenerator {
    @SerialName("flat") FLAT,
    @SerialName("normal") NORMAL
}

class World(val server: MinecraftServer, val name: String) : AutoCloseable {
    val worldDir = File("worlds", name).apply { mkdirs() }
    val metaFile = File(worldDir, "meta.json")
    val playersDir = File(worldDir, "players").apply { mkdirs() }
    val regionsDir = File(worldDir, "regions").apply { mkdirs() }

    @OptIn(ExperimentalSerializationApi::class)
    val meta = try {
        metaFile.inputStream().use { PRETTY_JSON.decodeFromStream(it) }
    } catch (e: Exception) {
        if (e !is FileNotFoundException) {
            LOGGER.warn("Couldn't read world meta, creating anew", e)
        }
        WorldMeta(server.config)
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun save() {
        metaFile.outputStream().use { PRETTY_JSON.encodeToStream(meta, it) }
    }

    override fun close() = save()
}

class WorldRegion(val world: World, val x: Int, val z: Int) : AutoCloseable {
    val regionFile = File(world.regionsDir, "region_${x}_${z}${world.meta.saveFormat.fileExtension}")

    @Serializable
    internal class RegionData(
        @SerialName("ChunksPresent") val chunksPresent: BitSet,
        @SerialName("Chunks")        val chunks: Array<WorldChunk.ChunkData>
    )

    private val chunks = arrayOfNulls<WorldChunk>(32 * 32)

    fun getChunk(x: Int, z: Int): WorldChunk? = chunks[x * 32 + z]

    internal fun toData(): RegionData {
        val chunksPresent = BitSet(chunks.size)
        val chunksData = mutableListOf<WorldChunk.ChunkData>()
        chunks.forEachIndexed { i, chunk ->
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
                val memIndex = x * 32 + z
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
    val x get() = region.x * 32 + xInRegion
    val z get() = region.z * 32 + zInRegion

    @Serializable
    internal class ChunkData(
        @SerialName("SectionsPresent") val sectionsPresent: BitSet,
        @SerialName("Sections")        val sections: Array<ChunkSection.SectionData>,
        @SerialName("Heightmap")       val heightmap: SimpleBitStorage
    )

    private val sections = arrayOfNulls<ChunkSection>(254)
    private val heightmap = SimpleBitStorage(12, 16 * 16)

    fun getSection(y: Int): ChunkSection? = sections[y + 127]

    internal fun toData(): ChunkData {
        val sectionsPresent = BitSet(sections.size)
        val sectionsData = mutableListOf<ChunkSection.SectionData>()
        sections.forEachIndexed { i, section ->
            if (section != null) {
                sectionsPresent.set(i)
                sectionsData.add(section.toData())
            }
        }
        return ChunkData(sectionsPresent, sectionsData.toTypedArray(), heightmap)
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
        @SerialName("Palette") val palette: List<Identifier>,
        @SerialName("Blocks")  val blocks: ByteArray
    )

    private val data = ByteArray(16 * 16 * 16)

    var blockCount = 0
        private set

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
        val localIds = ByteArray(Blocks.BLOCK_NUM_TO_ID.size) {
            paletteList.indexOf(Blocks.BLOCK_NUM_TO_ID[it + 1]).toByte()
        }
        val packedData = ByteArray(16 * 16 * 16)
        repeat(16 * 16 * 16) { i ->
            packedData[i] = localIds[data[i].toUByte().toInt()]
        }
        return SectionData(paletteList, packedData)
    }

    internal fun fromData(input: SectionData) {
        val palette = ByteArray(input.palette.size) {
            Blocks.BLOCK_ID_TO_NUM[input.palette[it]]?.toByte() ?:
                throw IllegalArgumentException("Unknown block ID ${input.palette[it]}")
        }
        repeat(16 * 16 * 16) { i ->
            data[i] = palette[input.blocks[i].toUByte().toInt()]
        }
    }
}

@Serializable
class WorldMeta() {
    var time = 0L
    var worldGenerator = WorldGenerator.NORMAL
    var saveFormat = SaveFormat.NBT

    constructor(config: ServerConfig) : this() {
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
}
