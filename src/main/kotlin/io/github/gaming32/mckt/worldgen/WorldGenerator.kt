package io.github.gaming32.mckt.worldgen

import io.github.gaming32.mckt.BlockAccess
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

abstract class WorldGenerator(val seed: Long, private val threaded: Boolean = true) {
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
