package io.github.gaming32.mckt.worldgen.phases

import io.github.gaming32.mckt.BlockAccess
import io.github.gaming32.mckt.Blocks
import io.github.gaming32.mckt.worldgen.DefaultWorldGenerator
import io.github.gaming32.mckt.worldgen.WorldgenPhase
import io.github.gaming32.mckt.worldgen.noise.PerlinNoise
import kotlin.random.Random

class SkyIslandsPhase(generator: DefaultWorldGenerator) : WorldgenPhase(generator) {
    companion object {
        const val OCTAVES = 3
        const val X_SCALE_ISLAND = 150.0
        const val Y_SCALE_ISLAND = 96.0
        const val Y_OFFSET_ISLAND = 1048
        const val X_SCALE_SURFACE = 225.0
        const val Y_SCALE_SURFACE = 32.0
        const val Y_OFFSET_SURFACE = 1080
    }

    private val perlin = PerlinNoise(Random(generator.seed).nextLong())

    private fun getIslandHeight(x: Int, z: Int) =
        (perlin.fbm2d(x / X_SCALE_ISLAND, z / X_SCALE_ISLAND, OCTAVES) * Y_SCALE_ISLAND).toInt() + Y_OFFSET_ISLAND

    private fun getSurfaceHeight(x: Int, z: Int) =
        (perlin.noise2d(x / X_SCALE_SURFACE, z / X_SCALE_SURFACE) * Y_SCALE_SURFACE).toInt() + Y_OFFSET_SURFACE

    override fun generateChunk(chunk: BlockAccess, chunkX: Int, chunkZ: Int) {
        val cx = chunkX shl 4
        val cz = chunkZ shl 4
        repeat(16) { x ->
            val absX = cx + x
            repeat(16) zLoop@ { z ->
                val absZ = cz + z
                val islandHeight = getIslandHeight(absX, absZ)
                val surfaceHeight = getSurfaceHeight(absX, absZ)
                if (islandHeight > surfaceHeight) return@zLoop
                for (y in islandHeight until surfaceHeight) {
                    if (surfaceHeight - y < 4) {
                        chunk.setBlockImmediate(x, y, z, Blocks.DIRT)
                    } else {
                        chunk.setBlockImmediate(x, y, z, Blocks.STONE)
                    }
                }
                chunk.setBlockImmediate(x, surfaceHeight, z, Blocks.GRASS_BLOCK)
            }
        }
    }
}
