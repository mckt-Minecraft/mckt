package io.github.gaming32.mckt.worldgen.phases

import io.github.gaming32.mckt.Blocks
import io.github.gaming32.mckt.WorldChunk
import io.github.gaming32.mckt.worldgen.DefaultWorldGenerator
import io.github.gaming32.mckt.worldgen.WorldgenPhase
import io.github.gaming32.mckt.worldgen.noise.OpenSimplex
import kotlin.random.Random

class CavesPhase(generator: DefaultWorldGenerator) : WorldgenPhase(generator) {
    companion object {
        const val BOUND = 0.4
        const val X_SCALE = 50.0
        const val Y_SCALE = 50.0
        const val Y_OFFSET = 0.0
        const val OCTAVES = 3
    }

    private val simplex = OpenSimplex(generator.seed)

    private fun noise(x: Double, y: Double, z: Double): Double {
        var result = 0.0
        var multiplier = 1.0
        repeat(OCTAVES) {
            result += simplex.noise3(x * multiplier, y * multiplier, z * multiplier)
            multiplier *= 2
        }
        return result
    }

    override fun generateChunk(chunk: WorldChunk, rand: Random) {
        val cx = chunk.x shl 4
        val cz = chunk.z shl 4
        repeat(16) { x ->
            val absX = cx + x
            repeat(16) { z ->
                val absZ = cz + z
                for (y in -2031..-80) {
                    if (chunk.getBlock(x, y, z) == Blocks.AIR) continue
                    val noise = noise(absX / X_SCALE, y / Y_SCALE, absZ / X_SCALE) + Y_OFFSET
                    if (noise <= BOUND) continue
                    chunk.setBlock(x, y, z, Blocks.AIR)
                }
            }
        }
    }
}
