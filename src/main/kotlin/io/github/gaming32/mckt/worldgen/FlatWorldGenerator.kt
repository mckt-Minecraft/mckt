package io.github.gaming32.mckt.worldgen

import io.github.gaming32.mckt.BlockAccess
import io.github.gaming32.mckt.Blocks

class FlatWorldGenerator(seed: Long) : WorldGenerator(seed, false) {
    override fun generateChunk(chunk: BlockAccess, chunkX: Int, chunkZ: Int) {
        repeat(16) { x ->
            repeat(16) { z ->
                chunk.setBlockImmediate(x, 0, z, Blocks.BEDROCK)
                chunk.setBlockImmediate(x, 1, z, Blocks.DIRT)
                chunk.setBlockImmediate(x, 2, z, Blocks.DIRT)
                chunk.setBlockImmediate(x, 3, z, Blocks.GRASS_BLOCK)
            }
        }
    }
}
