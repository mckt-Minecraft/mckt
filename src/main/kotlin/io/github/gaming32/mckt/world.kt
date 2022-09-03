package io.github.gaming32.mckt

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import net.benwoodworth.knbt.*
import java.io.File
import java.io.FileNotFoundException

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

class World(val server: MinecraftServer, val name: String) : AutoCloseable {
    val worldDir = File("worlds", name).apply { mkdirs() }
    val metaFile = File(worldDir, "meta.json")
    val playersDir = File(worldDir, "players").apply { mkdirs() }
    @OptIn(ExperimentalSerializationApi::class)
    val meta = try {
        metaFile.inputStream().use { PRETTY_JSON.decodeFromStream(it) }
    } catch (e: Exception) {
        if (e !is FileNotFoundException) {
            LOGGER.warn("Couldn't read world meta, creating anew", e)
        }
        WorldMeta()
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun save() {
        metaFile.outputStream().use { PRETTY_JSON.encodeToStream(meta, it) }
    }

    override fun close() = save()
}

@Serializable
class WorldMeta {
    var time = 0L
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
