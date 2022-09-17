package io.github.gaming32.mckt.packet

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

open class MinecraftInputStream(inp: InputStream) : DataInputStream(inp) {
    fun readString(maxLength: Int = 32767): String {
        val length = readVarInt()
        if (length > maxLength) {
            throw IllegalArgumentException("String exceeds maxLength ($maxLength bytes)")
        }
        val result = ByteArray(length)
        readFully(result)
        return result.decodeToString()
    }

    fun readText() = GsonComponentSerializer.gson().deserialize(readString(262144))

    fun readIdentifier() = Identifier.parse(readString(32767))

    fun readVarLong(): Long {
        var value = 0L
        var position = 0

        while (true) {
            val currentByte = readByte().toUByte().toInt()
            value = value or ((currentByte and VARINT_SEGMENT_BITS).toLong() shl position)

            if ((currentByte and VARINT_CONTINUE_BIT) == 0) break

            position += 7

            if (position >= 64) throw RuntimeException("VarLong is too big")
        }

        return value
    }

    fun readItemStack(): ItemStack? {
        if (!readBoolean()) return null
        val intItemId = readVarInt()
        return ItemStack(
            ITEM_PROTOCOL_TO_ID[intItemId] ?: throw IllegalArgumentException("Unknown item ID: $intItemId"),
            readUnsignedByte(),
            readNbtTag() as NbtCompound?
        )
    }

    fun readNbtTag() = NETWORK_NBT.decodeFromStream<NbtTag>(this)

    fun readBlockPosition() = BlockPosition.decodeFromLong(readLong())

    fun readDegrees() = readByte().toFloat() / 256.0 * 360.0

    fun readRadians() = readByte().toFloat() / 256.0 * 2 * PI

    fun readUuid() = UUID(readLong(), readLong())

    fun readBitSet(): BitSet {
        val result = LongArray(readVarInt())
        for (i in result.indices) {
            result[i] = readLong()
        }
        return BitSet.valueOf(result)
    }
}

interface MinecraftWritable {
    fun write(out: MinecraftOutputStream)
}

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

fun InputStream.readVarInt(): Int {
    var value = 0
    var position = 0

    while (true) {
        val currentByte = read()
        if (currentByte < 0) {
            throw EOFException()
        }
        value = value or (currentByte and VARINT_SEGMENT_BITS shl position)

        if ((currentByte and VARINT_CONTINUE_BIT) == 0) break

        position += 7

        if (position >= 32) throw RuntimeException("VarInt is too big")
    }

    return value
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

inline fun encodeData(builder: MinecraftOutputStream.() -> Unit): ByteArray =
    ByteArrayOutputStream().also { MinecraftOutputStream(it).builder() }.toByteArray()
