package io.github.gaming32.mckt.worldgen.phases

import io.github.gaming32.mckt.BlockAccess
import io.github.gaming32.mckt.Blocks
import io.github.gaming32.mckt.worldgen.DefaultWorldGenerator
import io.github.gaming32.mckt.worldgen.WorldgenPhase
import io.github.gaming32.mckt.worldgen.noise.PerlinNoise
import kotlin.random.Random

class GroundPhase(generator: DefaultWorldGenerator) : WorldgenPhase(generator) {
    companion object {
        const val OCTAVES = 3
        const val X_SCALE = 150.0
        const val Y_SCALE = 96.0
        const val Y_OFFSET = -32
    }

    private val perlin = PerlinNoise(generator.seed)

    internal fun getHeight(x: Int, z: Int) =
        (perlin.fbm2d(x / X_SCALE, z / X_SCALE, OCTAVES) * Y_SCALE).toInt() + Y_OFFSET

    override fun generateChunk(chunk: BlockAccess, chunkX: Int, chunkZ: Int) {
        val cx = chunkX shl 4
        val cz = chunkZ shl 4
        repeat(16) { x ->
            val absX = cx + x
            repeat(16) { z ->
                val height = getHeight(absX, cz + z)
                for (y in -2032 until height - 4) {
                    chunk.setBlockImmediate(x, y, z, Blocks.STONE)
                }
                for (y in height - 4 until height) {
                    chunk.setBlockImmediate(x, y, z, Blocks.DIRT)
                }
                chunk.setBlockImmediate(x, height, z, Blocks.GRASS_BLOCK)
            }
        }
    }
}
