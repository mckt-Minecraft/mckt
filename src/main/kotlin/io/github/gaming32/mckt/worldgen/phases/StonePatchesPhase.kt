package io.github.gaming32.mckt.worldgen.phases

import io.github.gaming32.mckt.BlockAccess
import io.github.gaming32.mckt.Blocks
import io.github.gaming32.mckt.WorldChunk
import io.github.gaming32.mckt.worldgen.DefaultWorldGenerator
import io.github.gaming32.mckt.worldgen.WorldgenPhase
import io.github.gaming32.mckt.worldgen.noise.PerlinNoise
import kotlin.random.Random

class StonePatchesPhase(generator: DefaultWorldGenerator) : WorldgenPhase(generator) {
    companion object {
        private const val FLIP_CONSTANT = -660517680413635759L
        private const val SCALE = 15.0
        private const val REQUIREMENT = 0.6
        private val PATCH_TYPES = arrayOf(Blocks.DIORITE, Blocks.ANDESITE, Blocks.GRANITE)
    }

    private val noises = Random(generator.seed xor FLIP_CONSTANT).let { rand ->
        Array(PATCH_TYPES.size) {
            PATCH_TYPES[it] to PerlinNoise(rand.nextLong())
        }
    }

    override fun generateChunk(chunk: BlockAccess, chunkX: Int, chunkZ: Int, rand: Random) {
        repeat(16) { x ->
            val absX = (chunkX shl 4) + x
            repeat(16) { z ->
                val absZ = (chunkZ shl 4) + z
                for (sectionY in -127..69) {
                    if (chunk is WorldChunk && chunk.getSection(sectionY) == null) continue
                    repeat(16) { y ->
                        val absY = (sectionY shl 4) + y
                        noises.forEach { (block, noise) ->
                            if (
                                noise.noise3d(absX / SCALE, absY / SCALE, absZ / SCALE) >= REQUIREMENT &&
                                chunk.getBlockImmediate(x, absY, z) == Blocks.STONE
                            ) {
                                chunk.setBlockImmediate(x, absY, z, block)
                            }
                        }
                    }
                }
            }
        }
    }
}