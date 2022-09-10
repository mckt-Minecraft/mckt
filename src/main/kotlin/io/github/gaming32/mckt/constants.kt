package io.github.gaming32.mckt

import io.github.gaming32.mckt.objects.Identifier
import io.github.gaming32.mckt.data.toGson
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

const val MINECRAFT_VERSION = "1.19.2"
const val PROTOCOL_VERSION = 760

@Serializable
class GameVersion(
    val minecraftVersion: String,
    val version: Int,
    val dataVersion: Int? = null,
    val usesNetty: Boolean,
    val majorVersion: String
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
val GLOBAL_PALETTE = (MinecraftServer::class.java.getResourceAsStream("/blocks.json")?.use { input ->
    Json.decodeFromStream<JsonObject>(input)
}?.asSequence()?.associate { (id, data) ->
    Identifier.parse(id) to data.toGson().asJsonObject["states"].asJsonArray.first {
        it.asJsonObject["default"]?.asBoolean ?: false
    }.asJsonObject["id"].asInt
} ?: mapOf()) + mapOf(null to 0)

@OptIn(ExperimentalSerializationApi::class)
private val REGISTRIES_DATA = MinecraftServer::class.java.getResourceAsStream("/registries.json")?.use { input ->
    Json.decodeFromStream<JsonObject>(input)
}?.asSequence()?.associate { (registry, data) ->
    Identifier.parse(registry) to data.toGson().asJsonObject["entries"].asJsonObject
} ?: mapOf()

val ITEM_ID_TO_PROTOCOL = REGISTRIES_DATA[Identifier("item")]?.entrySet()?.associate { (name, data) ->
    Identifier.parse(name) to data.asJsonObject["protocol_id"].asInt
} ?: mapOf()
val ITEM_PROTOCOL_TO_ID = ITEM_ID_TO_PROTOCOL.flip()
