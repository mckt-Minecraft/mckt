package io.github.gaming32.mckt.worldgen.defaultgen

import io.github.gaming32.mckt.BlockAccess
import io.github.gaming32.mckt.util.StringSerializable
import io.github.gaming32.mckt.util.StringSerializable.EmptySerializable
import io.github.gaming32.mckt.worldgen.WorldGenerator
import io.github.gaming32.mckt.worldgen.defaultgen.phases.*
import kotlin.random.Random

class DefaultWorldGenerator(
    seed: Long
) : WorldGenerator<DefaultWorldGenerator, EmptySerializable>(DefaultWorldGenerator, seed, EmptySerializable) {
    companion object Type : WorldGeneratorType<DefaultWorldGenerator, EmptySerializable>() {
        override val defaultConfig = EmptySerializable

        override fun createGenerator(seed: Long, config: EmptySerializable) = DefaultWorldGenerator(seed)

        override fun deserializeConfig(serialized: String) = StringSerializable.EmptySerializable
    }

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
