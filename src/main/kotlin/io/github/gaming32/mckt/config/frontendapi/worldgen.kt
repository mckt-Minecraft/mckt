package io.github.gaming32.mckt.config.frontendapi

import io.github.gaming32.mckt.objects.Identifier
import io.github.gaming32.mckt.util.StringSerializable
import io.github.gaming32.mckt.world.gen.WorldGenerator
import io.github.gaming32.mckt.world.gen.WorldGenerators
import io.github.gaming32.mckt.world.gen.defaultgen.DefaultWorldGenerator

data class WorldGeneratorConfig<T : WorldGenerator<T, C>, C : StringSerializable>(
    val generator: WorldGenerator.WorldGeneratorType<T, C>,
    val config: C = generator.defaultConfig
)

val defaultGenerator = WorldGeneratorConfig(DefaultWorldGenerator)

val flatGenerator = WorldGeneratorConfig(io.github.gaming32.mckt.world.gen.FlatWorldGenerator)

inline fun <T : WorldGenerator<T, C>, C : StringSerializable> generator(
    generator: WorldGenerator.WorldGeneratorType<T, C>,
    configSetter: () -> C = { generator.defaultConfig }
) = WorldGeneratorConfig(generator, configSetter())

fun generator(id: Identifier) = WorldGeneratorConfig(WorldGenerators.getGenerator(id))

inline fun flatGenerator(builder: io.github.gaming32.mckt.world.gen.FlatWorldGenerator.FlatConfig.Builder.() -> Unit) =
    generator(io.github.gaming32.mckt.world.gen.FlatWorldGenerator) {
        val result = io.github.gaming32.mckt.world.gen.FlatWorldGenerator.FlatConfig.Builder()
        result.builder()
        return@generator result.build()
    }
