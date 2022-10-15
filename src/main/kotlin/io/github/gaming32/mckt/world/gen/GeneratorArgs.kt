package io.github.gaming32.mckt.world.gen

import io.github.gaming32.mckt.world.BlockAccess
import io.github.gaming32.mckt.world.World
import io.github.gaming32.mckt.world.WorldChunk

data class GeneratorArgs(
    val chunk: BlockAccess,
    val world: World,
    val chunkX: Int,
    val chunkZ: Int
) {
    constructor(chunk: WorldChunk) : this(chunk, chunk.world, chunk.x, chunk.z)
}