package io.github.gaming32.mckt.objects

import io.github.gaming32.mckt.data.toGson
import io.github.gaming32.mckt.data.toKotlinx
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import net.benwoodworth.knbt.NbtCompound
import net.benwoodworth.knbt.StringifiedNbt
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

object SNbtCompoundSerializer : KSerializer<NbtCompound> {
    override val descriptor = PrimitiveSerialDescriptor("NbtCompound", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: NbtCompound) =
        encoder.encodeString(StringifiedNbt.encodeToString(value))

    override fun deserialize(decoder: Decoder): NbtCompound = StringifiedNbt.decodeFromString(decoder.decodeString())
}
