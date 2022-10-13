package io.github.gaming32.mckt.worldgen

import io.github.gaming32.mckt.objects.Identifier

typealias WorldGeneratorCreator = (seed: Long) -> WorldGenerator

object WorldGenerators {
    private val GENERATORS = mutableMapOf<Identifier, WorldGeneratorCreator>()

    fun getGenerator(id: Identifier) = GENERATORS[id]
        ?: throw IllegalArgumentException("Unknown world generator $id")

    fun register(id: Identifier, creator: WorldGeneratorCreator) {
        GENERATORS.put(id, creator)?.let {
            throw IllegalArgumentException("A world generator already exists with the ID $id")
        }
    }

    init {
        register(Identifier("mckt", "flat"), ::FlatWorldGenerator)
        register(Identifier("mckt", "default"), ::DefaultWorldGenerator)
        // Old world meta.json files get parsed as minecraft:normal
        register(Identifier("minecraft", "normal"), ::DefaultWorldGenerator)
    }
}
