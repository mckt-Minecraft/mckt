package io.github.gaming32.mckt.worldgen

import io.github.gaming32.mckt.BlockAccess
import io.github.gaming32.mckt.worldgen.phases.*
import kotlin.random.Random

class DefaultWorldGenerator(seed: Long) : WorldGenerator(seed) {
    val groundPhase = GroundPhase(this)
    val treePhase = TreeDecorationPhase(this)

    val phases = listOf(
        groundPhase,
        CavesPhase(this),
        SkyIslandsPhase(this),
        StonePatchesPhase(this),
        treePhase,
        BottomPhase(this)
    )

    override fun generateChunk(chunk: BlockAccess, chunkX: Int, chunkZ: Int) {
        phases.forEach { it.generateChunk(chunk, chunkX, chunkZ) }
    }

    fun getRandom(chunkX: Int, chunkZ: Int, phaseSeed: Long) =
        Random(seed xor (chunkX.toLong() shl 32) xor chunkZ.toLong() xor phaseSeed)
}
