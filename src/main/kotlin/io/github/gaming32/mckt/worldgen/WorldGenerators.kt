package io.github.gaming32.mckt.worldgen

import io.github.gaming32.mckt.objects.Identifier
import io.github.gaming32.mckt.worldgen.defaultgen.DefaultWorldGenerator
import io.michaelrocks.bimap.HashBiMap
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object WorldGenerators {
    object Serializer : KSerializer<WorldGenerator.WorldGeneratorType<*, *>> {
        override val descriptor = PrimitiveSerialDescriptor("WorldGeneratorType", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: WorldGenerator.WorldGeneratorType<*, *>) =
            encoder.encodeString(getId(value).toString())

        override fun deserialize(decoder: Decoder) = getGenerator(Identifier.parse(decoder.decodeString()))
    }

    private val GENERATORS = HashBiMap<Identifier, WorldGenerator.WorldGeneratorType<*, *>>()

    fun getGenerator(id: Identifier) = GENERATORS[id]
        ?: throw IllegalArgumentException("Unknown world generator $id")

    fun getId(type: WorldGenerator.WorldGeneratorType<*, *>) =
        GENERATORS.inverse[type]
            ?: throw IllegalArgumentException("World generator ${type.javaClass.name} is not registered")

    fun register(id: Identifier, type: WorldGenerator.WorldGeneratorType<*, *>) {
        GENERATORS.put(id, type)?.let {
            throw IllegalArgumentException("A world generator already exists with the ID $id")
        }
    }

    init {
        register(Identifier("mckt", "flat"), FlatWorldGenerator)
        register(Identifier("mckt", "default"), DefaultWorldGenerator)
    }
}
