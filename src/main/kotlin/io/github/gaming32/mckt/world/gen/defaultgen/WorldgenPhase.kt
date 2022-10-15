package io.github.gaming32.mckt.world.gen.defaultgen

import io.github.gaming32.mckt.world.BlockAccess

abstract class WorldgenPhase(val generator: DefaultWorldGenerator) {
    abstract fun generateChunk(chunk: BlockAccess, chunkX: Int, chunkZ: Int)
}
