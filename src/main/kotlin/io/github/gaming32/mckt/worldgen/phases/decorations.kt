package io.github.gaming32.mckt.worldgen.phases

import io.github.gaming32.mckt.BlockState
import io.github.gaming32.mckt.Blocks
import io.github.gaming32.mckt.WorldChunk
import io.github.gaming32.mckt.worldgen.DefaultWorldGenerator
import io.github.gaming32.mckt.worldgen.WorldgenPhase
import io.github.gaming32.mckt.worldgen.noise.PerlinNoise
import kotlin.random.Random

class TreeDecorationPhase(generator: DefaultWorldGenerator) : WorldgenPhase(generator) {
    companion object {
        private const val REQUIREMENT = 0.2
        private const val REQUIREMENT_2 = 0.5
        private const val FLIP_CONSTANT = 4009383296558120008L
        private const val SCALE = 200.0
    }

    private val perlin = PerlinNoise(generator.seed xor FLIP_CONSTANT)

    override fun generateChunk(chunk: WorldChunk, rand: Random) {
        val cx = chunk.x shl 4
        val cz = chunk.z shl 4
        val noise = perlin.noise2d(cx / SCALE, cz / SCALE)
        if (noise > REQUIREMENT) {
            val offsetX = rand.nextInt(12)
            val offsetZ = rand.nextInt(12)
            val absX = cx + offsetX
            val absZ = cz + offsetZ
            draw(chunk, rand, offsetX, generator.groundPhase.getHeight(absX + 2, absZ + 2) + 1, offsetZ)
            if (noise > REQUIREMENT_2) {
                var offsetX2 = rand.nextInt(12)
                if (offsetX2 in offsetX..offsetX + 4) {
                    offsetX2 = (offsetX + 5).mod(12)
                } else if (offsetX2 in offsetX - 4 until offsetX) {
                    offsetX2 = (offsetX - 5).mod(12)
                }
                var offsetZ2 = rand.nextInt(12)
                if (offsetZ2 in offsetZ..offsetZ + 4) {
                    offsetZ2 = (offsetZ + 5).mod(12)
                } else if (offsetZ2 in offsetZ - 4 until offsetZ) {
                    offsetZ2 = (offsetZ - 5).mod(12)
                }
                val absX2 = cx + offsetX2
                val absZ2 = cz + offsetZ2
                draw(chunk, rand, offsetX2, generator.groundPhase.getHeight(absX2 + 2, absZ2 + 2) + 1, offsetZ2)
            }
        }
    }

    private fun draw(chunk: WorldChunk, rand: Random, x: Int, y: Int, z: Int) {
        val endY = rand.nextInt(y + 3, y + 7)
        for (oy in y..endY) {
            chunk.setBlockIf(x + 2, oy, z + 2, Blocks.WOOD)
        }
        for (oy in endY - 2 until endY) {
            for (ox in 0 until 5) {
                for (oz in 0 until 5) {
                    if (
                        (ox == 2 && oz == 2) ||
                        (ox == 0 && oz == 0 && rand.nextBoolean()) ||
                        (ox == 4 && oz == 0 && rand.nextBoolean()) ||
                        (ox == 0 && oz == 4 && rand.nextBoolean()) ||
                        (ox == 4 && oz == 4 && rand.nextBoolean())
                    ) continue
                    chunk.setBlockIf(x + ox, oy, z + oz, Blocks.LEAVES)
                }
            }
        }
        chunk.setBlockIf(x + 1, endY, z + 2, Blocks.LEAVES)
        chunk.setBlockIf(x + 3, endY, z + 2, Blocks.LEAVES)
        chunk.setBlockIf(x + 2, endY, z + 1, Blocks.LEAVES)
        chunk.setBlockIf(x + 2, endY, z + 3, Blocks.LEAVES)
        if (rand.nextBoolean()) chunk.setBlockIf(x + 1, endY, z + 1, Blocks.LEAVES)
        if (rand.nextBoolean()) chunk.setBlockIf(x + 3, endY, z + 1, Blocks.LEAVES)
        if (rand.nextBoolean()) chunk.setBlockIf(x + 1, endY, z + 3, Blocks.LEAVES)
        if (rand.nextBoolean()) chunk.setBlockIf(x + 3, endY, z + 3, Blocks.LEAVES)
        chunk.setBlockIf(x + 1, endY + 1, z + 2, Blocks.LEAVES)
        chunk.setBlockIf(x + 3, endY + 1, z + 2, Blocks.LEAVES)
        chunk.setBlockIf(x + 2, endY + 1, z + 1, Blocks.LEAVES)
        chunk.setBlockIf(x + 2, endY + 1, z + 3, Blocks.LEAVES)
        chunk.setBlockIf(x + 2, endY + 1, z + 2, Blocks.LEAVES)
        if (x + 2 in 0..15 && z + 2 in 0..15) {
            chunk.setBlock(x + 2, y - 1, z + 2, Blocks.DIRT)
        }
    }

    private fun WorldChunk.setBlockIf(x: Int, y: Int, z: Int, block: BlockState) {
        if (getBlock(x, y, z) == Blocks.AIR) {
            setBlock(x, y, z, block)
        }
    }
}
