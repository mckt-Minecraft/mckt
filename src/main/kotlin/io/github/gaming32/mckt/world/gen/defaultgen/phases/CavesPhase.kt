package io.github.gaming32.mckt.world.gen.defaultgen.phases

import io.github.gaming32.mckt.world.BlockAccess
import io.github.gaming32.mckt.world.Blocks
import io.github.gaming32.mckt.world.gen.defaultgen.DefaultWorldGenerator
import io.github.gaming32.mckt.world.gen.defaultgen.WorldgenPhase
import io.github.gaming32.mckt.world.gen.noise.OpenSimplex

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

    override fun generateChunk(chunk: BlockAccess, chunkX: Int, chunkZ: Int) {
        val cx = chunkX shl 4
        val cz = chunkZ shl 4
        repeat(16) { x ->
            val absX = cx + x
            repeat(16) { z ->
                val absZ = cz + z
                for (y in -2031..-80) {
                    if (chunk.getBlockImmediate(x, y, z) == Blocks.AIR) continue
                    val noise = noise(absX / X_SCALE, y / Y_SCALE, absZ / X_SCALE) + Y_OFFSET
                    if (noise <= BOUND) continue
                    chunk.setBlockImmediate(x, y, z, Blocks.AIR)
                }
            }
        }
    }
}
