package io.github.gaming32.mckt.nbt

import io.github.gaming32.mckt.castOrNull
import io.github.gaming32.mckt.data.*
import net.kyori.adventure.nbt.*
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream

sealed interface NbtElement<B : BinaryTag, out D : NbtElement<B, D>> {
    val type: BinaryTagType<B>

    fun toAdventure(): B

    @Suppress("UNCHECKED_CAST")
    fun deepCopy() = this as D

    fun encode(out: OutputStream)
}

@JvmInline
value class NbtByte(val value: Byte) : NbtElement<ByteBinaryTag, NbtByte> {
    val booleanValue get() = value == 1.toByte()

    constructor(booleanValue: Boolean) : this(if (booleanValue) 1 else 0)

    override val type: BinaryTagType<ByteBinaryTag> get() = BinaryTagTypes.BYTE
    override fun toAdventure() = ByteBinaryTag.of(value)
    override fun encode(out: OutputStream) = out.write(value.toInt())
}

@JvmInline
value class NbtShort(val value: Short) : NbtElement<ShortBinaryTag, NbtShort> {
    override val type: BinaryTagType<ShortBinaryTag> get() = BinaryTagTypes.SHORT
    override fun toAdventure() = ShortBinaryTag.of(value)
    override fun encode(out: OutputStream) = out.writeShort(value.toInt())
}

@JvmInline
value class NbtInt(val value: Int) : NbtElement<IntBinaryTag, NbtInt> {
    override val type: BinaryTagType<IntBinaryTag> get() = BinaryTagTypes.INT
    override fun toAdventure() = IntBinaryTag.of(value)
    override fun encode(out: OutputStream) = out.writeInt(value)
}

@JvmInline
value class NbtLong(val value: Long) : NbtElement<LongBinaryTag, NbtLong> {
    override val type: BinaryTagType<LongBinaryTag> get() = BinaryTagTypes.LONG
    override fun toAdventure() = LongBinaryTag.of(value)
    override fun encode(out: OutputStream) = out.writeLong(value)
}

@JvmInline
value class NbtFloat(val value: Float) : NbtElement<FloatBinaryTag, NbtFloat> {
    override val type: BinaryTagType<FloatBinaryTag> get() = BinaryTagTypes.FLOAT
    override fun toAdventure() = FloatBinaryTag.of(value)
    override fun encode(out: OutputStream) = out.writeFloat(value)
}

@JvmInline
value class NbtDouble(val value: Double) : NbtElement<DoubleBinaryTag, NbtDouble> {
    override val type: BinaryTagType<DoubleBinaryTag> get() = BinaryTagTypes.DOUBLE
    override fun toAdventure() = DoubleBinaryTag.of(value)
    override fun encode(out: OutputStream) = out.writeDouble(value)
}

@JvmInline
value class NbtByteArray(val content: ByteArray) : NbtElement<ByteArrayBinaryTag, NbtByteArray> {
    constructor(size: Int) : this(ByteArray(size))

    override val type: BinaryTagType<ByteArrayBinaryTag> get() = BinaryTagTypes.BYTE_ARRAY
    override fun toAdventure() = ByteArrayBinaryTag.of(*content)
    override fun deepCopy() = NbtByteArray(content.copyOf())

    override fun encode(out: OutputStream) {
        out.writeInt(content.size)
        out.write(content)
    }
}

@JvmInline
value class NbtString(val value: String) : NbtElement<StringBinaryTag, NbtString> {
    override val type: BinaryTagType<StringBinaryTag> get() = BinaryTagTypes.STRING
    override fun toAdventure() = StringBinaryTag.of(value)

    override fun encode(out: OutputStream) {
        if (out is DataOutputStream) {
            out
        } else {
            DataOutputStream(out)
        }.writeUTF(value)
    }
}

typealias NbtListElement<T> = NbtElement<out BinaryTag, T>

class NbtList<T : NbtListElement<T>>(
    val content: MutableList<T> = mutableListOf()
) : NbtElement<ListBinaryTag, NbtList<T>>, MutableList<T> by content {
    override val type: BinaryTagType<ListBinaryTag> get() = BinaryTagTypes.LIST

    override fun toAdventure() = ListBinaryTag.builder().run {
        content.forEach {
            add(it.toAdventure())
        }
        build()
    }

    override fun deepCopy() = NbtList(content.toMutableList())

    override fun encode(out: OutputStream) {
        out.writeByte((if (content.size > 0) content[0].type else BinaryTagTypes.END).id().toInt())
        out.writeInt(content.size)
        content.forEach { it.encode(out) }
    }
}

class NbtCompound(
    val content: MutableMap<String, NbtElement<*, *>> = mutableMapOf()
) : NbtElement<CompoundBinaryTag, NbtCompound>, MutableMap<String, NbtElement<*, *>> by content {
    override val type: BinaryTagType<CompoundBinaryTag> get() = BinaryTagTypes.COMPOUND

    override fun toAdventure() = CompoundBinaryTag.builder().run {
        content.forEach { (key, value) ->
            put(key, value.toAdventure())
        }
        build()
    }

    override fun deepCopy() = NbtCompound(content.toMutableMap())

    override fun encode(out: OutputStream) {
        val dos = if (out is DataOutputStream) out else DataOutputStream(out)
        content.forEach { (key, value) ->
            dos.writeByte(value.type.id().toInt())
            dos.writeUTF(key)
            value.encode(dos)
        }
        dos.writeByte(BinaryTagTypes.END.id().toInt())
    }

    constructor(vararg content: Pair<String, NbtElement<*, *>>) : this(mutableMapOf(*content))

    fun getBoolean(key: String) = content[key]?.castOrNull<NbtByte>()?.booleanValue ?: false

    fun getByte(key: String) = content[key]?.castOrNull<NbtByte>()?.value ?: 0

    fun getInt(key: String) = content[key]?.castOrNull<NbtInt>()?.value ?: 0

    fun getString(key: String) = content[key]?.castOrNull<NbtString>()?.value ?: ""

    @Suppress("UNCHECKED_CAST")
    fun <T : NbtListElement<T>> getList(key: String) = content[key] as? NbtList<T> ?: NbtList()

    fun getCompound(key: String) = content[key] as? NbtCompound ?: NbtCompound()

    fun getLongArray(key: String) = content[key]?.castOrNull<NbtLongArray>()?.content ?: LongArray(0)

    fun putBoolean(key: String, value: Boolean) {
        content[key] = NbtByte(value)
    }

    fun putInt(key: String, value: Int) {
        content[key] = NbtInt(value)
    }

    fun putString(key: String, value: String) {
        content[key] = NbtString(value)
    }

    fun getType(key: String): BinaryTagType<out BinaryTag> = content[key]?.type ?: BinaryTagTypes.END

    fun contains(key: String, type: BinaryTagType<*>): Boolean {
        val check = getType(key)
        @Suppress("IntroduceWhenSubject") // Keep old code structure for now
        return when {
            check == type -> true
//            type == NbtTagType.ANY_NUMBER -> check in NbtTagType.BYTE..NbtTagType.DOUBLE
            else -> false
        }
    }
}

@JvmInline
value class NbtIntArray(val content: IntArray) : NbtElement<IntArrayBinaryTag, NbtIntArray> {
    constructor(size: Int) : this(IntArray(size))

    override val type: BinaryTagType<IntArrayBinaryTag> get() = BinaryTagTypes.INT_ARRAY
    override fun toAdventure() = IntArrayBinaryTag.of(*content)
    override fun deepCopy() = NbtIntArray(content.copyOf())

    override fun encode(out: OutputStream) {
        out.writeInt(content.size)
        content.forEach { out.writeInt(it) }
    }
}

@JvmInline
value class NbtLongArray(val content: LongArray) : NbtElement<LongArrayBinaryTag, NbtLongArray> {
    constructor(size: Int) : this(LongArray(size))

    override val type: BinaryTagType<LongArrayBinaryTag> get() = BinaryTagTypes.LONG_ARRAY
    override fun toAdventure() = LongArrayBinaryTag.of(*content)
    override fun deepCopy() = NbtLongArray(content.copyOf())

    override fun encode(out: OutputStream) {
        out.writeInt(content.size)
        content.forEach { out.writeLong(it) }
    }
}

typealias NbtDecoder = (InputStream) -> NbtElement<*, *>

private val DECODERS = mapOf<Byte, NbtDecoder>(
    BinaryTagTypes.BYTE.id() to { NbtByte(it.readByte()) },
    BinaryTagTypes.SHORT.id() to { NbtShort(it.readShort()) },
    BinaryTagTypes.INT.id() to { NbtInt(it.readInt()) },
    BinaryTagTypes.LONG.id() to { NbtLong(it.readLong()) },
    BinaryTagTypes.FLOAT.id() to { NbtFloat(it.readFloat()) },
    BinaryTagTypes.DOUBLE.id() to { NbtDouble(it.readDouble()) },
    BinaryTagTypes.BYTE_ARRAY.id() to { inp ->
        val result = ByteArray(inp.readInt())
        inp.readFully(result)
        NbtByteArray(result)
    },
    BinaryTagTypes.STRING.id() to { NbtString((if (it is DataInputStream) it else DataInputStream(it)).readUTF()) },
    BinaryTagTypes.LIST.id() to { inp ->
        val type = inp.readByte()
        if (type == BinaryTagTypes.END.id()) {
            inp.readInt()
            NbtList(mutableListOf())
        } else {
            val decoder = getNbtDecoder(type)
            val size = inp.readInt()
            val result = ArrayList<NbtListElement<*>>(size)
            repeat(size) { result.add(decoder(inp)) }
            NbtList(result)
        }
    },
    BinaryTagTypes.COMPOUND.id() to { inp ->
        val result = mutableMapOf<String, NbtElement<*, *>>()
        val dis = if (inp is DataInputStream) inp else DataInputStream(inp)
        while (true) {
            val type = dis.readByte()
            if (type == BinaryTagTypes.END.id()) {
                break
            }
            result[dis.readUTF()] = getNbtDecoder(type)(inp)
        }
        NbtCompound(result)
    },
    BinaryTagTypes.INT_ARRAY.id() to { inp -> NbtIntArray(IntArray(inp.readInt()) { inp.readInt() }) },
    BinaryTagTypes.LONG_ARRAY.id() to { inp -> NbtLongArray(LongArray(inp.readInt()) { inp.readLong() }) },
)

fun getNbtDecoder(type: Byte): NbtDecoder = DECODERS[type] ?: throw IllegalArgumentException("Unknown NBT type $type")
