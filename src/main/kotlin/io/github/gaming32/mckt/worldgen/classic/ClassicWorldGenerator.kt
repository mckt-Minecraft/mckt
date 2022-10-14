package io.github.gaming32.mckt.worldgen.classic

import io.github.gaming32.mckt.BlockAccess
import io.github.gaming32.mckt.util.StringSerializable.EmptySerializable
import io.github.gaming32.mckt.worldgen.WorldGenerator

typealias JuRandom = java.util.Random

class ClassicWorldGenerator(
    seed: Long
) : WorldGenerator<ClassicWorldGenerator, EmptySerializable>(ClassicWorldGenerator, seed, EmptySerializable) {
    companion object Type : WorldGeneratorType<ClassicWorldGenerator, EmptySerializable>() {
        override val defaultConfig = EmptySerializable

        override fun createGenerator(seed: Long, config: EmptySerializable) = ClassicWorldGenerator(seed)

        override fun deserializeConfig(serialized: String) = EmptySerializable
    }

    private val chunkProvider = ThreadLocal.withInitial { ChunkProviderGenerate(GenInfo(seed)) }

    override fun generateChunk(chunk: BlockAccess, chunkX: Int, chunkZ: Int) =
        chunkProvider.get().generateChunk(chunk, chunkX, chunkZ)
}
