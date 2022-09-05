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

@OptIn(ExperimentalSerializationApi::class)
object BitSetSerializer : KSerializer<BitSet> {
    private val delegateSerializer = serializer<LongArray>()
    override val descriptor = SerialDescriptor("BitSet", delegateSerializer.descriptor)

    override fun serialize(encoder: Encoder, value: BitSet) = encoder.encodeSerializableValue(
        delegateSerializer,
        value.toLongArray()
    )

    override fun deserialize(decoder: Decoder): BitSet =
        BitSet.valueOf(decoder.decodeSerializableValue(delegateSerializer))
}
