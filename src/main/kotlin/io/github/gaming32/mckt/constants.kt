package io.github.gaming32.mckt

import io.github.gaming32.mckt.data.toGson
import io.github.gaming32.mckt.objects.Identifier
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
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
data class BlockState(
    val blockId: Identifier = Identifier.EMPTY,
    @SerialName("id")
    val globalId: Int = -1,
    val properties: Map<String, String> = mapOf(),
    @SerialName("default")
    val defaultForId: Boolean = false
) {
    companion object {
        fun fromMap(map: Map<String, String>) = BlockState(
            blockId = Identifier.parse(map["blockId"] ?: throw IllegalArgumentException(
                "Missing blockId field from block state data"
            )),
            properties = map.toMutableMap().apply { remove("blockId") }
        )
    }

    fun toMap() = properties + ("blockId" to blockId.toString())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BlockState

        if (blockId != other.blockId) return false
        if (properties != other.properties) return false

        return true
    }

    override fun hashCode(): Int {
        var result = blockId.hashCode()
        result = 31 * result + properties.hashCode()
        return result
    }

    override fun toString() = buildString {
        append(blockId)
        if (properties.isNotEmpty()) {
            append('[')
            properties.entries.forEachIndexed { index, (name, value) ->
                if (index > 0) {
                    append(',')
                }
                append(name)
                append('=')
                append(value)
            }
            append(']')
        }
    }

    fun canonicalize(): BlockState {
        return ID_TO_BLOCKSTATE.getOrNull(
            BLOCKSTATE_TO_ID.getInt(this).takeIf { it != -1 } ?: return this
        ) ?: this
    }
}

@OptIn(ExperimentalSerializationApi::class)
val DEFAULT_TRANSLATIONS = (MinecraftServer::class.java.getResourceAsStream("/en_us.json")?.use { input ->
    Json.decodeFromStream<Map<String, String>>(input)
} ?: mapOf()) + mapOf(
    "commands.save.saving" to "Saving world \"%s\"",
    "commands.save.success" to "Saved world \"%s\" in %sms",
    "commands.help.failed" to "Unknown command: %s",
    "commands.op.success" to "Made %s a server operator (level %s)"
)

@OptIn(ExperimentalSerializationApi::class)
val GAME_VERSIONS = MinecraftServer::class.java.getResourceAsStream("/protocolVersions.json")?.use { input ->
    Json.decodeFromStream<List<GameVersion>>(input)
} ?: listOf()
val GAME_VERSIONS_BY_PROTOCOL = GAME_VERSIONS
    .asSequence()
    .filter(GameVersion::usesNetty)
    .associateBy(GameVersion::version)

// blocks.json and registries.json were generated from the Vanilla server JAR with its builtin tool

@OptIn(ExperimentalSerializationApi::class)
val GLOBAL_PALETTE = MinecraftServer::class.java.getResourceAsStream("/blocks.json")?.use { input ->
    Json.decodeFromStream<JsonObject>(input)
}?.asSequence()?.flatMap { (blockId, data) ->
    data as JsonObject
    val blockIdAsIdentifier = Identifier.parse(blockId)
    (data["states"] as JsonArray)
        .asSequence()
        .map { Json.decodeFromJsonElement<BlockState>(it).copy(blockId = blockIdAsIdentifier) }
}?.toSet() ?: emptySet()

val DEFAULT_BLOCKSTATES = GLOBAL_PALETTE
    .asSequence()
    .filter(BlockState::defaultForId)
    .associateBy(BlockState::blockId)

@Suppress("UNCHECKED_CAST")
val ID_TO_BLOCKSTATE = arrayOfNulls<BlockState>(GLOBAL_PALETTE.size).apply {
    GLOBAL_PALETTE.forEach { this[it.globalId] = it }
} as Array<BlockState>
val BLOCKSTATE_TO_ID = GLOBAL_PALETTE.associateWithTo(Object2IntOpenHashMap()) {
    it.globalId
}.apply { defaultReturnValue(-1) }

@OptIn(ExperimentalSerializationApi::class)
private val REGISTRIES_DATA = MinecraftServer::class.java.getResourceAsStream("/registries.json")?.use { input ->
    Json.decodeFromStream<JsonObject>(input)
}?.asSequence()?.associate { (registry, data) ->
    Identifier.parse(registry) to data.toGson().asJsonObject["entries"].asJsonObject
} ?: mapOf()

val ITEM_ID_TO_PROTOCOL = REGISTRIES_DATA[Identifier("item")]?.entrySet()?.associate { (name, data) ->
    Identifier.parse(name) to data.asJsonObject["protocol_id"].asInt
} ?: mapOf()
val ITEM_PROTOCOL_TO_ID = ITEM_ID_TO_PROTOCOL.invert()

@OptIn(ExperimentalSerializationApi::class)
val DEFAULT_TAGS = MinecraftServer::class.java.getResourceAsStream("/defaultTags.json")?.use { input ->
    Json.decodeFromStream<Map<Identifier, Map<Identifier, IntArray>>>(input)
} ?: mapOf()
