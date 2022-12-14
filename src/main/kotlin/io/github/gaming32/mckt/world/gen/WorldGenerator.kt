package io.github.gaming32.mckt.world.gen

import io.github.gaming32.mckt.util.StringSerializable
import io.github.gaming32.mckt.world.BlockAccess
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.coroutines.EmptyCoroutineContext

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

    open val threaded = true

    abstract fun generateChunk(chunk: BlockAccess, chunkX: Int, chunkZ: Int)

    suspend fun generateChunkThreaded(args: GeneratorArgs) = coroutineScope {
        launch(if (threaded) args.world.worldgenPool else EmptyCoroutineContext) {
            generateChunk(args.chunk, args.chunkX, args.chunkZ)
        }
    }
}
