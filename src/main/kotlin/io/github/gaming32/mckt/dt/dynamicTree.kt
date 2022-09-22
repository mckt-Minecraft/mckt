package io.github.gaming32.mckt.dt

import io.github.gaming32.mckt.castOrNull
import net.benwoodworth.knbt.*

sealed interface DtElement<out N : NbtTag, out D : DtElement<N, D>> {
    val nbtType: Int

    fun toNbt(): N

    @Suppress("UNCHECKED_CAST")
    fun deepCopy() = this as D
}

@JvmInline
value class DtByte(val value: Byte) : DtElement<NbtByte, DtByte> {
    val booleanValue get() = value == 1.toByte()

    constructor(booleanValue: Boolean) : this(if (booleanValue) 1 else 0)

    override val nbtType get() = NbtTagType.BYTE
    override fun toNbt() = NbtByte(value)
}

@JvmInline
value class DtShort(val value: Short) : DtElement<NbtShort, DtShort> {
    override val nbtType get() = NbtTagType.SHORT
    override fun toNbt() = NbtShort(value)
}

@JvmInline
value class DtInt(val value: Int) : DtElement<NbtInt, DtInt> {
    override val nbtType get() = NbtTagType.INT
    override fun toNbt() = NbtInt(value)
}

@JvmInline
value class DtLong(val value: Long) : DtElement<NbtLong, DtLong> {
    override val nbtType get() = NbtTagType.LONG
    override fun toNbt() = NbtLong(value)
}

@JvmInline
value class DtFloat(val value: Float) : DtElement<NbtFloat, DtFloat> {
    override val nbtType get() = NbtTagType.LONG
    override fun toNbt() = NbtFloat(value)
}

@JvmInline
value class DtDouble(val value: Double) : DtElement<NbtDouble, DtDouble> {
    override val nbtType get() = NbtTagType.DOUBLE
    override fun toNbt() = NbtDouble(value)
}

@JvmInline
value class DtByteArray(val content: ByteArray) : DtElement<NbtByteArray, DtByteArray> {
    constructor(size: Int) : this(ByteArray(size))

    override val nbtType get() = NbtTagType.BYTE_ARRAY
    override fun toNbt() = NbtByteArray(content)
    override fun deepCopy() = DtByteArray(content.copyOf())
}

@JvmInline
value class DtString(val value: String) : DtElement<NbtString, DtString> {
    override val nbtType get() = NbtTagType.STRING
    override fun toNbt() = NbtString(value)
}

@JvmInline
value class DtList<T : DtElement<NbtTag, T>>(
    val content: MutableList<T> = mutableListOf()
) : DtElement<NbtList<*>, DtList<T>>, MutableList<T> by content {
    override val nbtType get() = NbtTagType.LIST

    @Suppress("UNCHECKED_CAST") // Workaround to call constructor without known element type
    override fun toNbt(): NbtList<*> = NbtList(content as List<NbtByte>)

    override fun deepCopy() = DtList(content.toMutableList())
}

@JvmInline
value class DtCompound(
    val content: MutableMap<String, DtElement<*, *>> = mutableMapOf()
) : DtElement<NbtCompound, DtCompound>, MutableMap<String, DtElement<*, *>> by content {
    override val nbtType get() = NbtTagType.COMPOUND
    override fun toNbt() = NbtCompound(content.asSequence().associate { (key, value) -> key to value.toNbt() })
    override fun deepCopy() = DtCompound(content.toMutableMap())

    fun getCompound(key: String) = content[key] as? DtCompound ?: DtCompound()

    fun getString(key: String) = content[key]?.castOrNull<DtString>()?.value ?: ""

    fun putString(key: String, value: String) {
        content[key] = DtString(value)
    }

    fun getType(key: String) = content[key]?.nbtType ?: NbtTagType.NULL

    fun contains(key: String, type: Int): Boolean {
        val check = getType(key)
        return when {
            check == type -> true
            type == NbtTagType.ANY_NUMBER -> check in NbtTagType.BYTE..NbtTagType.DOUBLE
            else -> false
        }
    }
}

@JvmInline
value class DtIntArray(val content: IntArray) : DtElement<NbtIntArray, DtIntArray> {
    constructor(size: Int) : this(IntArray(size))

    override val nbtType get() = NbtTagType.INT_ARRAY
    override fun toNbt() = NbtIntArray(content)
    override fun deepCopy() = DtIntArray(content.copyOf())
}

@JvmInline
value class DtLongArray(val content: LongArray) : DtElement<NbtLongArray, DtLongArray> {
    constructor(size: Int) : this(LongArray(size))

    override val nbtType get() = NbtTagType.LONG_ARRAY
    override fun toNbt() = NbtLongArray(content)
    override fun deepCopy() = DtLongArray(content.copyOf())
}
