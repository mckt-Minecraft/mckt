@file:OptIn(ExperimentalSerializationApi::class)

package io.github.gaming32.mckt

import io.github.gaming32.mckt.data.toGson
import io.github.gaming32.mckt.objects.BlockState
import io.github.gaming32.mckt.objects.Identifier
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

const val MINECRAFT_VERSION = "1.19.2"
const val PROTOCOL_VERSION = 760
val MCKT_VERSION = MinecraftServer::class.java.`package`.specificationVersion ?: "UNKNOWN"

@Serializable
data class GameVersion(
    val minecraftVersion: String,
    val version: Int,
    val dataVersion: Int? = null,
    val usesNetty: Boolean,
    val majorVersion: String
)

@Serializable
data class BlockSoundGroup(
    val volume: Float,
    val pitch: Float,
    val breakSound: Identifier,
    val stepSound: Identifier,
    val placeSound: Identifier,
    val hitSound: Identifier,
    val fallSound: Identifier
)

@Serializable
enum class PistonBehavior {
    @SerialName("normal") NORMAL,
    @SerialName("destroy") DESTROY,
    @SerialName("block") BLOCK,
    @SerialName("ignore") IGNORE,
    @SerialName("push_only") PUSH_ONLY
}

@Serializable
data class BlockMaterial(
    val mapColor: Int,
    val pistonBehavior: PistonBehavior,
    val blocksMovement: Boolean,
    val burnable: Boolean,
    val liquid: Boolean,
    val blocksLight: Boolean,
    val replaceable: Boolean,
    val solid: Boolean
)

@Serializable
sealed class BlockTypeProperties {
    @Serializable
    @SerialName("block")
    object DefaultBlockType : BlockTypeProperties()

    @Serializable
    @SerialName("pillar")
    object PillarBlockType : BlockTypeProperties()

    @Serializable
    @SerialName("sapling")
    object SaplingBlockType : BlockTypeProperties()

    @Serializable
    @SerialName("pane")
    object PaneBlockType : BlockTypeProperties()

    @Serializable
    @SerialName("door")
    object DoorBlockType : BlockTypeProperties()

    @Serializable
    @SerialName("slab")
    object SlabBlockType : BlockTypeProperties()

    @Serializable
    @SerialName("stairs")
    data class StairsBlockType(
        @Serializable(BlockState.CanonicalSerializer::class)
        val baseBlockState: BlockState
    ) : BlockTypeProperties()

    @Serializable
    @SerialName("trapdoor")
    object TrapdoorBlockType : BlockTypeProperties()
}

@Serializable
data class BlockProperties(
    val soundGroup: BlockSoundGroup,
    val material: BlockMaterial,
    val mapColor: Int,
    val blastResistance: Double,
    val hardness: Double,
    val typeProperties: BlockTypeProperties
)

val DEFAULT_TRANSLATIONS = (MinecraftServer::class.java.getResourceAsStream("/en_us.json")?.use {
    Json.decodeFromStream<Map<String, String>>(it)
} ?: mapOf()) + mapOf(
    "commands.save.saving" to "Saving world \"%s\"",
    "commands.save.success" to "Saved world \"%s\" in %sms",
    "commands.help.failed" to "Unknown command: %s",
    "commands.op.success" to "Made %s a server operator (level %s)"
)

val GAME_VERSIONS = MinecraftServer::class.java.getResourceAsStream("/protocolVersions.json")?.use {
    Json.decodeFromStream<List<GameVersion>>(it)
} ?: listOf()
val GAME_VERSIONS_BY_PROTOCOL = GAME_VERSIONS
    .asSequence()
    .filter(GameVersion::usesNetty)
    .associateBy(GameVersion::version)

private val REGISTRIES_DATA = MinecraftServer::class.java.getResourceAsStream("/registries.json")?.use {
    Json.decodeFromStream<JsonObject>(it)
}?.asSequence()?.associate { (registry, data) ->
    Identifier.parse(registry) to data.toGson().asJsonObject["entries"].asJsonObject
} ?: mapOf()

val ITEM_ID_TO_PROTOCOL = REGISTRIES_DATA[Identifier("item")]?.entrySet()?.associate { (name, data) ->
    Identifier.parse(name) to data.asJsonObject["protocol_id"].asInt
} ?: mapOf()
val ITEM_PROTOCOL_TO_ID = ITEM_ID_TO_PROTOCOL.invert()

val DEFAULT_TAGS = MinecraftServer::class.java.getResourceAsStream("/defaultTags.json")?.use { input ->
    Json.decodeFromStream<Map<Identifier, Map<Identifier, IntArray>>>(input)
} ?: mapOf()

val BLOCK_SOUND_GROUPS = MinecraftServer::class.java.getResourceAsStream("/dataexport/blockSoundGroups.json")?.use {
    Json.decodeFromStream<Map<String, BlockSoundGroup>>(it)
} ?: mapOf()

val BLOCK_MATERIALS = MinecraftServer::class.java.getResourceAsStream("/dataexport/materials.json")?.use {
    Json.decodeFromStream<Map<String, BlockMaterial>>(it)
} ?: mapOf()

val BLOCK_PROPERTIES = MinecraftServer::class.java.getResourceAsStream("/dataexport/blocks.json")?.use {
    Json.decodeFromStream<Map<Identifier, JsonObject>>(it).asSequence().associate { (id, data) ->
        val soundGroup = data["soundGroup"].cast<JsonPrimitive>().content
        val material = data["material"].cast<JsonPrimitive>().content
        id to BlockProperties(
            BLOCK_SOUND_GROUPS[soundGroup] ?: throw IllegalArgumentException("Unknown sound group: $soundGroup"),
            BLOCK_MATERIALS[material] ?: throw IllegalArgumentException("Unknown material: $material"),
            data["mapColor"].cast<JsonPrimitive>().int,
            data["blastResistance"].cast<JsonPrimitive>().double,
            data["hardness"].cast<JsonPrimitive>().double,
            Json.decodeFromJsonElement(data["typeProperties"].cast())
        )
    }
} ?: mapOf()

