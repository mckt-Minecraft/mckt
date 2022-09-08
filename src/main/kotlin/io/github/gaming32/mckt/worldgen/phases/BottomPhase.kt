package io.github.gaming32.mckt.worldgen.phases

import io.github.gaming32.mckt.Blocks
import io.github.gaming32.mckt.WorldChunk
import io.github.gaming32.mckt.worldgen.DefaultWorldGenerator
import io.github.gaming32.mckt.worldgen.WorldgenPhase
import kotlin.random.Random

class BottomPhase(generator: DefaultWorldGenerator) : WorldgenPhase(generator) {
    override fun generateChunk(chunk: WorldChunk, rand: Random) {
        repeat(16) { x ->
            repeat(16) { z ->
                chunk.setBlock(x, -2032, z, Blocks.BEDROCK)
                if (rand.nextInt(4) != 0) chunk.setBlock(x, -2031, z, Blocks.BEDROCK)
                if (rand.nextBoolean()) chunk.setBlock(x, -2030, z, Blocks.BEDROCK)
                if (rand.nextInt(4) == 0) chunk.setBlock(x, -2029, z, Blocks.BEDROCK)
                for (y in -2028..-2017) {
                    if (chunk.getBlock(x, y, z) == null) {
                        chunk.setBlock(x, y, z, Blocks.LAVA)
                    }
                }
            }
        }
    }
}
