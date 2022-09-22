@file:OptIn(ExperimentalContracts::class)

package io.github.gaming32.mckt.data

import io.github.gaming32.mckt.ITEM_ID_TO_PROTOCOL
import io.github.gaming32.mckt.ITEM_PROTOCOL_TO_ID
import io.github.gaming32.mckt.NETWORK_NBT
import io.github.gaming32.mckt.dt.DtCompound
import io.github.gaming32.mckt.dt.toDt
import io.github.gaming32.mckt.objects.*
import io.ktor.utils.io.*
import net.benwoodworth.knbt.NbtTag
import net.benwoodworth.knbt.decodeFromStream
import net.benwoodworth.knbt.encodeToStream
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import java.io.*
import java.util.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.PI

private const val VARINT_SEGMENT_BITS = 0x7f
private const val VARINT_CONTINUE_BIT = 0x80

interface Writable {
    fun write(out: OutputStream)
}

//region Readers
fun InputStream.readFully(b: ByteArray) = readFully(b, 0, b.size)

fun InputStream.readFully(b: ByteArray, off: Int, len: Int) {
    if (len < 0) throw IndexOutOfBoundsException()
    var n = 0
    while (n < len) {
        val count = read(b, off + n, len - n)
        if (count < 0) throw EOFException()
        n += count
    }
}

fun InputStream.readByte(): Byte {
    val result = read()
    if (result < 0) {
        throw EOFException()
    }
    return result.toByte()
}

fun InputStream.readUByte() = readByte().toUByte()

fun InputStream.readBoolean() = readByte() != 0.toByte()

fun InputStream.readShort(): Short {
    val ch1: Int = read()
    val ch2: Int = read()
    if (ch1 or ch2 < 0) throw EOFException()
    return ((ch1 shl 8) + (ch2 shl 0)).toShort()
}

fun InputStream.readUShort() = readShort().toUShort()

fun InputStream.readInt(): Int {
    val ch1: Int = read()
    val ch2: Int = read()
    val ch3: Int = read()
    val ch4: Int = read()
    if (ch1 or ch2 or ch3 or ch4 < 0) throw EOFException()
    return (ch1 shl 24) + (ch2 shl 16) + (ch3 shl 8) + (ch4 shl 0)
}

fun InputStream.readFloat() = Float.fromBits(readInt())

fun InputStream.readLong() = if (this is DataInputStream) {
    readLong()
} else {
    val buffer = ByteArray(8)
    readFully(buffer)
    (buffer[0].toLong() shl 56) +
        ((buffer[1].toInt() and 255).toLong() shl 48) +
        ((buffer[2].toInt() and 255).toLong() shl 40) +
        ((buffer[3].toInt() and 255).toLong() shl 32) +
        ((buffer[4].toInt() and 255).toLong() shl 24) +
        (buffer[5].toInt() and 255 shl 16) +
        (buffer[6].toInt() and 255 shl 8) +
        (buffer[7].toInt() and 255 shl 0)
}

fun InputStream.readDouble() = Double.fromBits(readLong())

fun InputStream.readVarInt(): Int {
    var value = 0
    var position = 0

    while (true) {
        val currentByte = readUByte().toInt()
        value = value or (currentByte and VARINT_SEGMENT_BITS shl position)

        if ((currentByte and VARINT_CONTINUE_BIT) == 0) break

        position += 7

        if (position >= 32) throw RuntimeException("VarInt is too big")
    }

    return value
}

fun InputStream.readVarLong(): Long {
    var value = 0L
    var position = 0

    while (true) {
        val currentByte = readUByte().toInt()
        value = value or ((currentByte and VARINT_SEGMENT_BITS).toLong() shl position)

        if ((currentByte and VARINT_CONTINUE_BIT) == 0) break

        position += 7

        if (position >= 64) throw RuntimeException("VarLong is too big")
    }

    return value
}

fun InputStream.readByteArray() = ByteArray(readVarInt()).also { readFully(it) }

fun InputStream.readLongArray() = LongArray(readVarInt()) { readLong() }

fun InputStream.readString(maxLength: Int = 32767): String {
    val length = readVarInt()
    if (length > maxLength) {
        throw IllegalArgumentException("String exceeds maxLength ($maxLength bytes)")
    }
    val result = ByteArray(length)
    readFully(result)
    return result.decodeToString()
}

fun InputStream.readText() = GsonComponentSerializer.gson().deserialize(readString(262144))

fun InputStream.readIdentifier() = Identifier.parse(readString(32767))

inline fun <reified T : Enum<T>> InputStream.readEnum() = enumValues<T>()[readVarInt()]

inline fun <reified T : Enum<T>> InputStream.readByteEnum() = enumValues<T>()[readUByte().toInt()]

fun InputStream.readItemStack(): ItemStack {
    if (!readBoolean()) return ItemStack.EMPTY
    val intItemId = readVarInt()
    return ItemStack(
        ITEM_PROTOCOL_TO_ID[intItemId] ?: throw IllegalArgumentException("Unknown item ID: $intItemId"),
        readUByte().toInt(),
        readNbtTag().toDt() as DtCompound
    )
}

fun InputStream.readNbtTag() = NETWORK_NBT.decodeFromStream<NbtTag>(this)

fun InputStream.readBlockPosition() = BlockPosition.decodeFromLong(readLong())

fun InputStream.readDegrees() = readUByte().toFloat() / 256f * 360f

fun InputStream.readRadians() = readUByte().toFloat() / 128f * PI.toFloat()

fun InputStream.readUuid() = UUID(readLong(), readLong())

fun InputStream.readBitSet(): BitSet = BitSet.valueOf(readLongArray())

fun InputStream.readBlockHitResult(): BlockHitResult {
    val location = readBlockPosition()
    val direction = readEnum<Direction>()
    return BlockHitResult(
        Vector3d(
            location.x.toDouble() + readFloat().toDouble(),
            location.y.toDouble() + readFloat().toDouble(),
            location.z.toDouble() + readFloat().toDouble()
        ),
        location, direction, readBoolean()
    )
}
//endregion

//region Writers
@Suppress("NOTHING_TO_INLINE")
inline fun OutputStream.writeByte(b: Int) = write(b)

fun OutputStream.writeBoolean(b: Boolean) = write(if (b) 1 else 0)

fun OutputStream.writeShort(i: Int) {
    write(i ushr 8 and 0xFF)
    write(i ushr 0 and 0xFF)
}

fun OutputStream.writeInt(i: Int) {
    write(i ushr 24 and 0xFF)
    write(i ushr 16 and 0xFF)
    write(i ushr 8 and 0xFF)
    write(i ushr 0 and 0xFF)
}

fun OutputStream.writeFloat(f: Float) = writeInt(f.toRawBits())

fun OutputStream.writeLong(l: Long) = if (this is DataOutputStream) {
    writeLong(l)
} else {
    val buffer = ByteArray(8)
    buffer[0] = (l ushr 56).toByte()
    buffer[1] = (l ushr 48).toByte()
    buffer[2] = (l ushr 40).toByte()
    buffer[3] = (l ushr 32).toByte()
    buffer[4] = (l ushr 24).toByte()
    buffer[5] = (l ushr 16).toByte()
    buffer[6] = (l ushr 8).toByte()
    buffer[7] = (l ushr 0).toByte()
    write(buffer, 0, 8)
}

fun OutputStream.writeDouble(d: Double) = writeLong(d.toRawBits())

fun OutputStream.writeVarInt(i: Int) {
    var value = i
    while (true) {
        if ((value and VARINT_SEGMENT_BITS.inv()) == 0) {
            return write(value)
        }

        write(value and VARINT_SEGMENT_BITS or VARINT_CONTINUE_BIT)

        value = value ushr 7
    }
}

fun OutputStream.writeVarLong(l: Long) {
    var value = l
    while (true) {
        if ((value and VARINT_SEGMENT_BITS.toLong().inv()) == 0L) {
            return write(value.toInt())
        }

        write((value and VARINT_SEGMENT_BITS.toLong() or VARINT_CONTINUE_BIT.toLong()).toInt())

        value = value ushr 7
    }
}

fun OutputStream.writeLongArray(array: LongArray) {
    writeVarInt(array.size)
    array.forEach { writeLong(it) }
}

fun OutputStream.writeVarIntArray(array: IntArray) {
    writeVarInt(array.size)
    array.forEach { writeVarInt(it) }
}

inline fun <T> OutputStream.writeArray(array: Array<T>, writer: OutputStream.(T) -> Unit) {
    writeVarInt(array.size)
    array.forEach { writer(it) }
}

inline fun <T> OutputStream.writeArray(array: List<T>, writer: OutputStream.(T) -> Unit) {
    writeVarInt(array.size)
    array.forEach { writer(it) }
}

inline fun <K, V> OutputStream.writeArray(array: Map<K, V>, writer: OutputStream.(K, V) -> Unit) {
    writeVarInt(array.size)
    array.forEach { writer(it.key, it.value) }
}

inline fun <T> OutputStream.writeOptional(v: T?, writer: OutputStream.(T) -> Unit) {
    contract {
        callsInPlace(writer, InvocationKind.AT_MOST_ONCE)
    }
    writeBoolean(v != null)
    v?.let { writer(it) }
}

fun OutputStream.writeString(s: String, maxLength: Int = 32767) {
    val encoded = s.encodeToByteArray()
    if (encoded.size > maxLength) {
        throw IllegalArgumentException("String exceeds maxLength ($maxLength bytes)")
    }
    writeVarInt(encoded.size)
    write(encoded)
}

fun OutputStream.writeOptionalString(s: String?, maxLength: Int = 32767) {
    writeBoolean(s != null)
    s?.let { writeString(it, maxLength) }
}

fun OutputStream.writeText(text: Component) = writeString(GsonComponentSerializer.gson().serialize(text), 262144)

fun OutputStream.writeOptionalText(text: Component?) {
    writeBoolean(text != null)
    text?.let { writeText(it) }
}

fun OutputStream.writeIdentifier(id: Identifier) = writeString(id.toString(), 32767)

fun OutputStream.writeIdentifierArray(ids: List<Identifier>) = writeArray(ids) { writeIdentifier(it) }

fun <T : Enum<T>> OutputStream.writeEnum(v: T) = writeVarInt(v.ordinal)

fun <T : Enum<T>> OutputStream.writeByteEnum(v: T) = writeByte(v.ordinal)

fun OutputStream.writeItemStack(item: ItemStack) {
    writeBoolean(item.isNotEmpty())
    if (item.isNotEmpty()) {
        writeVarInt(ITEM_ID_TO_PROTOCOL[item.itemId] ?: throw IllegalArgumentException("Unknown item ID: ${item.itemId}"))
        writeVarInt(item.count)
        val dt = item.extraNbt
        if (dt.isNullOrEmpty()) {
            write(0) // TAG_End
        } else {
            writeNbtTag(dt.toNbt())
        }
    }
}

fun OutputStream.writeItemStackArray(items: Array<out ItemStack>) = writeArray(items) { writeItemStack(it) }

fun OutputStream.writeNbtTag(tag: NbtTag) = NETWORK_NBT.encodeToStream(NbtTag.serializer(), tag, this)

fun OutputStream.writeBlockPosition(pos: BlockPosition) = writeLong(pos.encodeToLong())

fun OutputStream.writeDegrees(degrees: Float) = write((degrees / 360f * 256f).toInt())

fun OutputStream.writeRadians(radians: Float) = write((radians * 128f / PI.toFloat()).toInt())

fun OutputStream.writeUuid(uuid: UUID) {
    writeLong(uuid.mostSignificantBits)
    writeLong(uuid.leastSignificantBits)
}

fun OutputStream.writeBitSet(bits: BitSet) = writeLongArray(bits.toLongArray())

fun OutputStream.writeBlockHitResult(hit: BlockHitResult) {
    val location = hit.location
    writeBlockPosition(location)
    writeEnum(hit.side)
    val position = hit.position
    writeFloat((position.x - location.x.toDouble()).toFloat())
    writeFloat((position.y - location.y.toDouble()).toFloat())
    writeFloat((position.z - location.z.toDouble()).toFloat())
    writeBoolean(hit.insideBlock)
}
//endregion

//region Ktor IO extensions
suspend fun ByteWriteChannel.writeVarInt(i: Int) {
    var value = i
    while (true) {
        if ((value and VARINT_SEGMENT_BITS.inv()) == 0) {
            return writeByte(value)
        }

        writeByte(value and VARINT_SEGMENT_BITS or VARINT_CONTINUE_BIT)

        value = value ushr 7
    }
}

suspend fun ByteReadChannel.readVarInt(specialFe: Boolean = false): Int {
    var value = 0
    var position = 0

    while (true) {
        val currentByte = readByte().toUByte().toInt()
        if (specialFe && currentByte == 0xFE && position == 0) return 0xFE
        value = value or (currentByte and VARINT_SEGMENT_BITS shl position)

        if ((currentByte and VARINT_CONTINUE_BIT) == 0) break

        position += 7

        if (position >= 32) throw RuntimeException("VarInt is too big")
    }

    return value
}
//endregion

inline fun encodeData(builder: OutputStream.() -> Unit): ByteArray =
    ByteArrayOutputStream().also { it.builder() }.toByteArray()
