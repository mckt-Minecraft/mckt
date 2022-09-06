package io.github.gaming32.mckt.worldgen

import io.github.gaming32.mckt.WorldChunk
import io.github.gaming32.mckt.worldgen.phases.GroundPhase
import io.github.gaming32.mckt.worldgen.phases.TreeDecorationPhase

class DefaultWorldGenerator(val seed: Long) {
    val groundPhase = GroundPhase(this)
    val phases = listOf(
        groundPhase,
        TreeDecorationPhase(this)
    )

    fun generateChunk(chunk: WorldChunk) {
        phases.forEach { it.generateChunk(chunk) }
    }
}
