package io.github.gaming32.mckt.objects

import io.github.gaming32.mckt.packet.toGson
import io.github.gaming32.mckt.packet.toKotlinx
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.serializer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import java.util.*

object IdentifierSerializer : KSerializer<Identifier> {
    override val descriptor = PrimitiveSerialDescriptor("Identifier", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Identifier) = encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder) = Identifier.parse(decoder.decodeString())
}

object UUIDSerializer : KSerializer<UUID> {
    override val descriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UUID) = encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder) = UUID.fromString(decoder.decodeString())!!
}

@OptIn(ExperimentalSerializationApi::class)
object TextSerializer : KSerializer<Component> {
    private val delegateSerializer = serializer<JsonElement>()
    override val descriptor = SerialDescriptor("Component", delegateSerializer.descriptor)

    override fun serialize(encoder: Encoder, value: Component) = encoder.encodeSerializableValue(
        delegateSerializer,
        GsonComponentSerializer.gson().serializeToTree(value).toKotlinx()
    )

    override fun deserialize(decoder: Decoder) =
        GsonComponentSerializer.gson().deserializeFromTree(decoder.decodeSerializableValue(delegateSerializer).toGson())
}
