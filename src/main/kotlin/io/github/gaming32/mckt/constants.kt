package io.github.gaming32.mckt

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream

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
val GAME_VERSIONS = GameVersion::class.java.getResourceAsStream("/protocolVersions.json")?.use { input ->
    Json.decodeFromStream<List<GameVersion>>(input)
} ?: listOf()
val GAME_VERSIONS_BY_PROTOCOL = GAME_VERSIONS.associateBy { it.version }
