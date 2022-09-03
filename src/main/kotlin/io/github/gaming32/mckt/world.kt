@file:OptIn(ExperimentalSerializationApi::class)

package io.github.gaming32.mckt

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.File

private val LOGGER = getLogger()

class World(val server: MinecraftServer, val name: String) : AutoCloseable {
    val worldDir = File("worlds", name).absoluteFile
    val metaFile = File(worldDir, "meta.json")
    val meta: WorldMeta

    init {
        worldDir.mkdirs()
        meta = try {
            metaFile.inputStream().use { PRETTY_JSON.decodeFromStream(it) }
        } catch (e: Exception) {
            LOGGER.warn("Couldn't load world meta, creating anew", e)
            WorldMeta()
        }
    }

    fun save() {
        metaFile.outputStream().use { PRETTY_JSON.encodeToStream(meta, it) }
    }

    override fun close() = save()
}

@Serializable
class WorldMeta {
    var time = 0L
}
