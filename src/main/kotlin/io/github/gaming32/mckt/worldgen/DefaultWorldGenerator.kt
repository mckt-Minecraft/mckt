package io.github.gaming32.mckt.worldgen

import io.github.gaming32.mckt.WorldChunk
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
        TreeDecorationPhase(this),
        BottomPhase(this)
    )

    fun generateChunk(chunk: WorldChunk) {
        val rand = Random(seed xor (chunk.x.toLong() shl 32) xor chunk.z.toLong() xor RAND_FLIP)
        phases.forEach { it.generateChunk(chunk, rand) }
    }
}
