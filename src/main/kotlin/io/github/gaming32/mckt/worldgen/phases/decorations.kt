package io.github.gaming32.mckt.worldgen.phases

import io.github.gaming32.mckt.Blocks
import io.github.gaming32.mckt.WorldChunk
import io.github.gaming32.mckt.objects.Identifier
import io.github.gaming32.mckt.worldgen.DefaultWorldGenerator
import io.github.gaming32.mckt.worldgen.WorldgenPhase
import io.github.gaming32.mckt.worldgen.noise.PerlinNoise
import kotlin.random.Random

class TreeDecorationPhase(generator: DefaultWorldGenerator) : WorldgenPhase(generator) {
    companion object {
        private const val REQUIREMENT = 0.5
        private const val FLIP_CONSTANT = 4009383296558120008L
        private const val SCALE = 150.0
    }

    private val perlin = PerlinNoise(generator.seed xor FLIP_CONSTANT)

    override fun generateChunk(chunk: WorldChunk) {
        val cx = chunk.x shl 4
        val cz = chunk.z shl 4
        if (perlin.noise2d(cx / SCALE, cz / SCALE) > REQUIREMENT) {
            val rand = Random(generator.seed xor (cx.toLong() shl 32) xor cz.toLong() xor FLIP_CONSTANT)
            val offsetX = rand.nextInt(12)
            val offsetZ = rand.nextInt(12)
            val absX = cx + offsetX
            val absZ = cz + offsetZ
            draw(chunk, rand, offsetX, generator.groundPhase.getHeight(absX + 2, absZ + 2) + 1, offsetZ)
        }
    }

    private fun draw(chunk: WorldChunk, rand: Random, x: Int, y: Int, z: Int) {
        chunk.setBlockIf(x + 2, y, z + 2, Blocks.WOOD)
        chunk.setBlockIf(x + 2, y + 1, z + 2, Blocks.WOOD)
        chunk.setBlockIf(x + 2, y + 2, z + 2, Blocks.WOOD)
        chunk.setBlockIf(x + 2, y + 3, z + 2, Blocks.WOOD)
        for (ox in 0 until 5) {
            for (oz in 0 until 5) {
                if (ox == 2 && oz == 2) continue
                chunk.setBlockIf(x + ox, y + 3, z + oz, Blocks.LEAVES)
            }
        }
        chunk.setBlockIf(x + 2, y + 4, z + 2, Blocks.LEAVES)
        chunk.setBlockIf(x + 1, y + 4, z + 2, Blocks.LEAVES)
        chunk.setBlockIf(x + 3, y + 4, z + 2, Blocks.LEAVES)
        chunk.setBlockIf(x + 2, y + 4, z + 1, Blocks.LEAVES)
        chunk.setBlockIf(x + 2, y + 4, z + 3, Blocks.LEAVES)
        if (rand.nextBoolean()) chunk.setBlockIf(x + 1, y + 4, z + 1, Blocks.LEAVES)
        if (rand.nextBoolean()) chunk.setBlockIf(x + 3, y + 4, z + 1, Blocks.LEAVES)
        if (rand.nextBoolean()) chunk.setBlockIf(x + 1, y + 4, z + 3, Blocks.LEAVES)
        if (rand.nextBoolean()) chunk.setBlockIf(x + 3, y + 4, z + 3, Blocks.LEAVES)
        chunk.setBlockIf(x + 2, y + 5, z + 2, Blocks.LEAVES)
    }

    private fun WorldChunk.setBlockIf(x: Int, y: Int, z: Int, block: Identifier?) {
        if (x < 0 || x > 15) return
        if (z < 0 || z > 15) return
        if (getBlock(x, y, z) == null) {
            setBlock(x, y, z, block)
        }
    }
}