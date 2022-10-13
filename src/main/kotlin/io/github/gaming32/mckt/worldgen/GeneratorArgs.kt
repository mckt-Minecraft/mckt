package io.github.gaming32.mckt.worldgen

import io.github.gaming32.mckt.BlockAccess
import io.github.gaming32.mckt.World
import io.github.gaming32.mckt.WorldChunk

data class GeneratorArgs(
    val chunk: BlockAccess,
    val world: World,
    val chunkX: Int,
    val chunkZ: Int
) {
    constructor(chunk: WorldChunk) : this(chunk, chunk.world, chunk.x, chunk.z)
}