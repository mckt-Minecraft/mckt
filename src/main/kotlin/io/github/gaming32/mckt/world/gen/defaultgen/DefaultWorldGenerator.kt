package io.github.gaming32.mckt.world.gen.defaultgen

import io.github.gaming32.mckt.util.StringSerializable.EmptySerializable
import io.github.gaming32.mckt.world.BlockAccess
import io.github.gaming32.mckt.world.gen.WorldGenerator
import io.github.gaming32.mckt.world.gen.defaultgen.phases.*
import kotlin.random.Random

class DefaultWorldGenerator(
    seed: Long
) : WorldGenerator<DefaultWorldGenerator, EmptySerializable>(DefaultWorldGenerator, seed, EmptySerializable) {
    companion object Type : WorldGeneratorType<DefaultWorldGenerator, EmptySerializable>() {
        override val defaultConfig = EmptySerializable

        override fun createGenerator(seed: Long, config: EmptySerializable) = DefaultWorldGenerator(seed)

        override fun deserializeConfig(serialized: String) = EmptySerializable
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
