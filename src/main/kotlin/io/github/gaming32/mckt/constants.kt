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
