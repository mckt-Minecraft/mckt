package io.github.gaming32.mckt.world

import io.github.gaming32.mckt.config.ServerConfig
import io.github.gaming32.mckt.objects.BlockPosition
import io.github.gaming32.mckt.objects.Identifier
import io.github.gaming32.mckt.world.gen.WorldGenerators
import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
class WorldMeta() {
    var time = 0L
    var seed = 0L
    var spawnPos: BlockPosition? = null
    var worldGenerator: Identifier = Identifier("mckt", "default")
    var generatorConfig: String = ""
    var autosave = true

    constructor(config: ServerConfig) : this() {
        seed = config.seed ?: Random.nextLong()
        val gen = config.defaultWorldGenerator
        worldGenerator = WorldGenerators.getId(gen.generator)
        generatorConfig = gen.config.serializeToString()
    }
}