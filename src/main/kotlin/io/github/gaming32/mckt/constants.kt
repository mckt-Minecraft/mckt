@file:OptIn(ExperimentalSerializationApi::class)

package io.github.gaming32.mckt

import io.github.gaming32.mckt.data.toGson
import io.github.gaming32.mckt.objects.Identifier
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromStream

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
data class BlockProperties(
    val soundGroup: String,
    val mapColor: Int,
    val blastResistance: Double,
    val hardness: Double
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

val BLOCK_PROPERTIES = MinecraftServer::class.java.getResourceAsStream("/dataexport/blocks.json")?.use {
    Json.decodeFromStream<Map<Identifier, BlockProperties>>(it)
} ?: mapOf()

