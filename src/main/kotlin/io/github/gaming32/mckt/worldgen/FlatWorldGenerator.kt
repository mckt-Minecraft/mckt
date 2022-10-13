package io.github.gaming32.mckt.worldgen

import io.github.gaming32.mckt.BlockAccess
import io.github.gaming32.mckt.Blocks
import io.github.gaming32.mckt.objects.BlockState
import io.github.gaming32.mckt.util.StringSerializable
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class FlatWorldGenerator(
    seed: Long, config: FlatConfig
) : WorldGenerator<FlatWorldGenerator, FlatWorldGenerator.FlatConfig>(FlatWorldGenerator, seed, config) {
    @Serializable
    class FlatConfig(
        val minY: Int = 0,
        val layers: List<
            @Serializable(BlockState.CanonicalSerializer::class)
            BlockState
        > = listOf(Blocks.BEDROCK, Blocks.DIRT, Blocks.DIRT, Blocks.GRASS_BLOCK)
    ) : StringSerializable {
        class Builder {
            var minY: Int = 0
            val layers = mutableListOf<BlockState>()

            inline fun layers(builder: MutableList<BlockState>.() -> Unit) {
                layers.builder()
            }

            fun build() = FlatConfig(minY, layers.toList())
        }

        override fun serializeToString() = Json.encodeToString(this)
    }

    companion object Type : WorldGeneratorType<FlatWorldGenerator, FlatConfig>() {
        override val defaultConfig = FlatConfig()

        override fun createGenerator(seed: Long, config: FlatConfig) = FlatWorldGenerator(seed, config)

        override fun deserializeConfig(serialized: String): FlatConfig {
            if (serialized.isEmpty()) return defaultConfig
            return Json.decodeFromString(serialized)
        }
    }

    override val threaded = false

    override fun generateChunk(chunk: BlockAccess, chunkX: Int, chunkZ: Int) {
        var y = config.minY
        for (block in config.layers) {
            repeat(16) { x ->
                repeat(16) { z ->
                    chunk.setBlockImmediate(x, y, z, block)
                }
            }
            y++
        }
    }
}
