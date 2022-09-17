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
                chunk.setBlock3Way(x, -2031, z, rand.nextInt(4) != 0)
                chunk.setBlock3Way(x, -2030, z, rand.nextBoolean())
                chunk.setBlock3Way(x, -2029, z, rand.nextInt(4) == 0)
                for (y in -2028..-2017) {
                    if (chunk.getBlock(x, y, z) == Blocks.AIR) {
                        chunk.setBlock(x, y, z, Blocks.LAVA)
                    }
                }
            }
        }
    }

    private fun WorldChunk.setBlock3Way(x: Int, y: Int, z: Int, yes: Boolean) {
        if (yes) {
            setBlock(x, y, z, Blocks.BEDROCK)
        } else if (getBlock(x, y, z) == Blocks.AIR) {
            setBlock(x, y, z, Blocks.LAVA)
        }
    }
}
