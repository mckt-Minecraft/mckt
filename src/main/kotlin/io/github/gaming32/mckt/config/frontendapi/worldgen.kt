package io.github.gaming32.mckt.config.frontendapi

import io.github.gaming32.mckt.objects.Identifier
import io.github.gaming32.mckt.util.StringSerializable
import io.github.gaming32.mckt.worldgen.FlatWorldGenerator
import io.github.gaming32.mckt.worldgen.WorldGenerator
import io.github.gaming32.mckt.worldgen.WorldGenerators
import io.github.gaming32.mckt.worldgen.defaultgen.DefaultWorldGenerator

data class WorldGeneratorConfig<T : WorldGenerator<T, C>, C : StringSerializable>(
    val generator: WorldGenerator.WorldGeneratorType<T, C>,
    val config: C = generator.defaultConfig
)

val defaultGenerator = WorldGeneratorConfig(DefaultWorldGenerator)

val flatGenerator = WorldGeneratorConfig(FlatWorldGenerator)

inline fun <T : WorldGenerator<T, C>, C : StringSerializable> generator(
    generator: WorldGenerator.WorldGeneratorType<T, C>,
    configSetter: () -> C = { generator.defaultConfig }
) = WorldGeneratorConfig(generator, configSetter())

fun generator(id: Identifier) = WorldGeneratorConfig(WorldGenerators.getGenerator(id))

inline fun flatGenerator(builder: FlatWorldGenerator.FlatConfig.Builder.() -> Unit) =
    generator(FlatWorldGenerator) {
        val result = FlatWorldGenerator.FlatConfig.Builder()
        result.builder()
        return@generator result.build()
    }
