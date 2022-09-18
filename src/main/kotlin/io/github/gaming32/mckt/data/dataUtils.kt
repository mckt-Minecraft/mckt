package io.github.gaming32.mckt.data

import io.github.gaming32.mckt.ITEM_ID_TO_PROTOCOL
import io.github.gaming32.mckt.ITEM_PROTOCOL_TO_ID
import io.github.gaming32.mckt.NETWORK_NBT
import io.github.gaming32.mckt.objects.BlockPosition
import io.github.gaming32.mckt.objects.Identifier
import io.github.gaming32.mckt.objects.ItemStack
import io.ktor.utils.io.*
import net.benwoodworth.knbt.NbtCompound
import net.benwoodworth.knbt.NbtTag
import net.benwoodworth.knbt.decodeFromStream
import net.benwoodworth.knbt.encodeToStream
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import java.io.*
import java.util.*
import kotlin.math.PI

private const val VARINT_SEGMENT_BITS = 0x7f
private const val VARINT_CONTINUE_BIT = 0x80

open class MinecraftOutputStream(out: OutputStream) : DataOutputStream(out) {
    fun writeString(s: String, maxLength: Int = 32767) {
        val encoded = s.encodeToByteArray()
        if (encoded.size > maxLength) {
            throw IllegalArgumentException("String exceeds maxLength ($maxLength bytes)")
        }
        writeVarInt(encoded.size)
        write(encoded)
    }

    fun writeText(text: Component) = writeString(GsonComponentSerializer.gson().serialize(text), 262144)

    fun writeIdentifier(id: Identifier) = writeString(id.toString(), 32767)

    fun writeVarInt(i: Int) {
        var value = i
        while (true) {
            if ((value and VARINT_SEGMENT_BITS.inv()) == 0) {
                return write(value)
            }

            write(value and VARINT_SEGMENT_BITS or VARINT_CONTINUE_BIT)

            value = value ushr 7
        }
    }

    fun writeVarLong(l: Long) {
        var value = l
        while (true) {
            if ((value and VARINT_SEGMENT_BITS.toLong().inv()) == 0L) {
                return write(value.toInt())
            }

            write((value and VARINT_SEGMENT_BITS.toLong() or VARINT_CONTINUE_BIT.toLong()).toInt())

            value = value ushr 7
        }
    }

    fun writeItemStack(item: ItemStack?) {
        writeBoolean(item != null && item.count > 0)
        if (item != null && item.count > 0) {
            writeVarInt(ITEM_ID_TO_PROTOCOL[item.itemId] ?: throw IllegalArgumentException("Unknown item ID: ${item.itemId}"))
            writeVarInt(item.count)
            if (item.extraNbt.isNullOrEmpty()) {
                writeByte(0) // TAG_End
            } else {
                writeNbtTag(item.extraNbt)
            }
        }
    }

    fun writeNbtTag(tag: NbtTag) = NETWORK_NBT.encodeToStream(NbtTag.serializer(), tag, this)

    fun writeBlockPosition(pos: BlockPosition) = writeLong(pos.encodeToLong())

    fun writeDegrees(degrees: Float) = write((degrees / 360.0 * 256.0).toInt())

    fun writeRadians(radians: Float) = write((radians / 2.0 / PI * 256.0).toInt())

    fun writeUuid(uuid: UUID) {
        writeLong(uuid.mostSignificantBits)
        writeLong(uuid.leastSignificantBits)
    }

    fun writeBitSet(bits: BitSet) {
        val data = bits.toLongArray()
        writeVarInt(data.size)
        for (l in data) {
            writeLong(l)
        }
    }
}

interface MinecraftWritable {
    fun write(out: MinecraftOutputStream)
}

//region Readers
fun InputStream.readFully(b: ByteArray) = readFully(b, 0, b.size)

fun InputStream.readFully(b: ByteArray, off: Int, len: Int) {
    if (len < 0) throw IndexOutOfBoundsException()
    var n = 0
    while (n < len) {
        val count: Int = read(b, off + n, len - n)
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

inline fun <reified T : Enum<T>> InputStream.readVarIntEnum() = enumValues<T>()[readVarInt()]

inline fun <reified T : Enum<T>> InputStream.readUByteEnum() = enumValues<T>()[readUByte().toInt()]

fun InputStream.readItemStack(): ItemStack? {
    if (!readBoolean()) return null
    val intItemId = readVarInt()
    return ItemStack(
        ITEM_PROTOCOL_TO_ID[intItemId] ?: throw IllegalArgumentException("Unknown item ID: $intItemId"),
        readUByte().toInt(),
        readNbtTag() as NbtCompound?
    )
}

fun InputStream.readNbtTag() = NETWORK_NBT.decodeFromStream<NbtTag>(this)

fun InputStream.readBlockPosition() = BlockPosition.decodeFromLong(readLong())

fun InputStream.readDegrees() = readUByte().toFloat() / 256f * 360f

fun InputStream.readRadians() = readUByte().toFloat() / 128f * PI.toFloat()

fun InputStream.readUuid() = UUID(readLong(), readLong())

fun InputStream.readBitSet(): BitSet = BitSet.valueOf(readLongArray())
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

inline fun encodeData(builder: MinecraftOutputStream.() -> Unit): ByteArray =
    ByteArrayOutputStream().also { MinecraftOutputStream(it).builder() }.toByteArray()
