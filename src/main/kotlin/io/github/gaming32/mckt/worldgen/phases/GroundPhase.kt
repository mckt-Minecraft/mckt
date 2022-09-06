package io.github.gaming32.mckt.worldgen.phases

import io.github.gaming32.mckt.Blocks
import io.github.gaming32.mckt.WorldChunk
import io.github.gaming32.mckt.worldgen.DefaultWorldGenerator
import io.github.gaming32.mckt.worldgen.WorldgenPhase
import io.github.gaming32.mckt.worldgen.noise.PerlinNoise

class GroundPhase(generator: DefaultWorldGenerator) : WorldgenPhase(generator) {
    companion object {
        const val OCTAVES = 3
        const val X_SCALE = 150.0
        const val Y_SCALE = 96.0
        const val Y_OFFSET = -32
    }

    private val perlin = PerlinNoise(generator.seed)

    override fun generateChunk(chunk: WorldChunk) {
        val cx = chunk.x shl 4
        val cz = chunk.z shl 4
        repeat(16) { x ->
            val absX = cx + x
            repeat(16) { z ->
                val height = (perlin.fbm2d(absX / X_SCALE, (cz + z) / X_SCALE, OCTAVES) * Y_SCALE).toInt() + Y_OFFSET
                for (y in -2032 until height - 4) {
                    chunk.setBlock(x, y, z, Blocks.STONE)
                }
                for (y in height - 4 until height) {
                    chunk.setBlock(x, y, z, Blocks.DIRT)
                }
                chunk.setBlock(x, height, z, Blocks.GRASS_BLOCK)
            }
        }
    }
}
