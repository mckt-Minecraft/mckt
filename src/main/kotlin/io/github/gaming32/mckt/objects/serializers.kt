@file:OptIn(ExperimentalSerializationApi::class)

package io.github.gaming32.mckt.objects

import io.github.gaming32.mckt.data.toGson
import io.github.gaming32.mckt.data.toKotlinx
import io.github.gaming32.mckt.nbt.NbtCompound
import io.github.gaming32.mckt.nbt.toNbt
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import net.kyori.adventure.nbt.TagStringIO
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import java.util.*

object UUIDSerializer : KSerializer<UUID> {
    override val descriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UUID) = encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder) = UUID.fromString(decoder.decodeString())!!
}

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

object SnbtCompoundSerializer : KSerializer<NbtCompound> {
    override val descriptor = PrimitiveSerialDescriptor("CompoundBinaryTag", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: NbtCompound) =
        encoder.encodeString(TagStringIO.get().asString(value.toAdventure()))

    override fun deserialize(decoder: Decoder) = TagStringIO.get().asCompound(decoder.decodeString())!!.toNbt()
}

object BlockPositionSerializer : KSerializer<BlockPosition> {
    private val delegateSerializer = serializer<IntArray>()
    override val descriptor = SerialDescriptor("BlockPosition", delegateSerializer.descriptor)

    override fun serialize(encoder: Encoder, value: BlockPosition) = encoder.encodeSerializableValue(
        delegateSerializer,
        intArrayOf(value.x, value.y, value.z)
    )

    override fun deserialize(decoder: Decoder): BlockPosition {
        val value = decoder.decodeSerializableValue(delegateSerializer)
        return BlockPosition(value[0], value[1], value[2])
    }
}

object Vector3dSerializer : KSerializer<Vector3d> {
    private val delegateSerializer = serializer<DoubleArray>()
    override val descriptor = SerialDescriptor("Vector3d", delegateSerializer.descriptor)

    override fun serialize(encoder: Encoder, value: Vector3d) = encoder.encodeSerializableValue(
        delegateSerializer,
        doubleArrayOf(value.x, value.y, value.z)
    )

    override fun deserialize(decoder: Decoder): Vector3d {
        val value = decoder.decodeSerializableValue(delegateSerializer)
        return Vector3d(value[0], value[1], value[2])
    }
}
