package io.github.gaming32.mckt.objects

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = Identifier.IdentifierSerializer::class)
data class Identifier(val namespace: String, val value: String) {
    companion object {
        fun parse(s: String) = s.indexOf(':').let { colonIndex ->
            if (colonIndex == -1) Identifier(s)
            else Identifier(s.substring(0, colonIndex), s.substring(colonIndex + 1))
        }
    }

    internal object IdentifierSerializer : KSerializer<Identifier> {
        override val descriptor = PrimitiveSerialDescriptor("Identifier", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: Identifier) = encoder.encodeString(value.toString())

        override fun deserialize(decoder: Decoder) = parse(decoder.decodeString())
    }

    init {
        if (namespace != "minecraft") {
            for (c in namespace) {
                if (
                    (c in 'a'..'z') ||
                    (c in '0'..'9') ||
                    c == '.' || c == '-' || c == '_'
                ) continue
                throw IllegalArgumentException("Invalid namespace $namespace")
            }
        }
        for (c in value) {
            if (
                (c in 'a'..'z') ||
                (c in '0'..'9') ||
                c == '.' || c == '-' || c == '_' || c == '/'
            ) continue
            throw IllegalArgumentException("Invalid value $value")
        }
    }

    constructor(value: String) : this("minecraft", value)

    override fun toString() = "$namespace:$value"
}
