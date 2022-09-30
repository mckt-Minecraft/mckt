package io.github.gaming32.mckt.objects

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import io.github.gaming32.mckt.commands.wrap
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.kyori.adventure.text.Component

@Serializable(Identifier.IdentifierSerializer::class)
data class Identifier(val namespace: String, val value: String) {
    companion object {
        val EMPTY = Identifier("", "")

        private val COMMAND_EXCEPTION = SimpleCommandExceptionType(
            Component.translatable("argument.id.invalid").wrap()
        )

        fun parse(s: String) = s.indexOf(':').let { colonIndex ->
            if (colonIndex == -1) Identifier(s)
            else Identifier(s.substring(0, colonIndex), s.substring(colonIndex + 1))
        }

        fun parse(reader: StringReader): Identifier {
            val cursor = reader.cursor
            while (reader.canRead()) {
                val c = reader.peek()
                if (isBaseValid(c) || c == ':' || c == '/') {
                    reader.skip()
                } else {
                    break
                }
            }
            try {
                return parse(reader.string.substring(cursor, reader.cursor))
            } catch (e: IllegalArgumentException) {
                reader.cursor = cursor
                throw COMMAND_EXCEPTION.createWithContext(reader)
            }
        }

        fun isBaseValid(c: Char) =
            (c in 'a'..'z') ||
            (c in '0'..'9') ||
            c == '.' || c == '-' || c == '_'
    }

    internal object IdentifierSerializer : KSerializer<Identifier> {
        override val descriptor = PrimitiveSerialDescriptor("Identifier", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: Identifier) = encoder.encodeString(value.toString())

        override fun deserialize(decoder: Decoder) = parse(decoder.decodeString())
    }

    init {
        if (namespace != "minecraft") {
            for (c in namespace) {
                if (isBaseValid(c)) continue
                throw IllegalArgumentException("Invalid namespace $namespace")
            }
        }
        for (c in value) {
            if (isBaseValid(c) || c == '/') continue
            throw IllegalArgumentException("Invalid value $value")
        }
    }

    constructor(value: String) : this("minecraft", value)

    override fun toString() = "$namespace:$value"

    fun toShortString() = if (namespace == "minecraft") value else "$namespace:$value"
}
