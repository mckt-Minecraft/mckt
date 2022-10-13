package io.github.gaming32.mckt.worldgen

import io.github.gaming32.mckt.BlockAccess
import io.github.gaming32.mckt.util.StringSerializable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

abstract class WorldGenerator<T : WorldGenerator<T, C>, C : StringSerializable>(
    val type: WorldGeneratorType<T, C>,
    val seed: Long,
    val config: C
) {
    @Serializable(WorldGenerators.Serializer::class)
    abstract class WorldGeneratorType<T : WorldGenerator<T, C>, C : StringSerializable> {
        abstract val defaultConfig: C

        abstract fun createGenerator(seed: Long, config: C = defaultConfig): T

        @JvmName("createGeneratorWithoutGenerics")
        @Suppress("UNCHECKED_CAST")
        fun createGenerator(seed: Long, config: StringSerializable) = createGenerator(seed, config as C)

        abstract fun deserializeConfig(serialized: String): C
    }

    open val threaded: Boolean = true

    abstract fun generateChunk(chunk: BlockAccess, chunkX: Int, chunkZ: Int)

    suspend fun generateChunkThreaded(args: GeneratorArgs) {
        if (threaded) {
            coroutineScope {
                launch(args.world.worldgenPool) {
                    generateChunk(args.chunk, args.chunkX, args.chunkZ)
                }
            }
        } else {
            generateChunk(args.chunk, args.chunkX, args.chunkZ)
        }
    }
}
