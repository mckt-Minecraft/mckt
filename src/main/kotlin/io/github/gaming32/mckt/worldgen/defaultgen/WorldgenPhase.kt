package io.github.gaming32.mckt.worldgen.defaultgen

import io.github.gaming32.mckt.BlockAccess

abstract class WorldgenPhase(val generator: DefaultWorldGenerator) {
    abstract fun generateChunk(chunk: BlockAccess, chunkX: Int, chunkZ: Int)
}
