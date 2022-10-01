package io.github.gaming32.mckt.worldgen

import io.github.gaming32.mckt.BlockAccess
import io.github.gaming32.mckt.worldgen.phases.*
import kotlin.random.Random

class DefaultWorldGenerator(val seed: Long) {
    companion object {
        private const val RAND_FLIP = 3402855461729105818L
    }

    val groundPhase = GroundPhase(this)
    val phases = listOf(
        groundPhase,
        CavesPhase(this),
        SkyIslandsPhase(this),
        StonePatchesPhase(this),
        TreeDecorationPhase(this),
        BottomPhase(this)
    )

    fun generateChunk(chunk: BlockAccess, chunkX: Int, chunkZ: Int) {
        val rand = Random(seed xor (chunkX.toLong() shl 32) xor chunkZ.toLong() xor RAND_FLIP)
        phases.forEach { it.generateChunk(chunk, chunkX, chunkZ, rand) }
    }
}
