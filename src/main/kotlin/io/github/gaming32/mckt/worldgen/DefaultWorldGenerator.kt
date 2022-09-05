package io.github.gaming32.mckt.worldgen

import io.github.gaming32.mckt.WorldChunk
import io.github.gaming32.mckt.worldgen.phases.GroundPhase

class DefaultWorldGenerator(val seed: Long) {
    val phases = listOf(
        GroundPhase(this)
    )

    fun generateChunk(chunk: WorldChunk) {
        phases.forEach { it.generateChunk(chunk) }
    }
}
