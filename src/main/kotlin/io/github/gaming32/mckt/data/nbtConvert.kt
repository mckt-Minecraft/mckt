package io.github.gaming32.mckt.data

import net.benwoodworth.knbt.*

fun NbtTag.toBaseKotlin(): Any = when (this) {
    is NbtByte -> value
    is NbtShort -> value
    is NbtInt -> value
    is NbtLong -> value
    is NbtFloat -> value
    is NbtDouble -> value
    is NbtString -> value
    is NbtByteArray -> toBaseKotlin()
    is NbtIntArray -> toBaseKotlin()
    is NbtLongArray -> toBaseKotlin()
    is NbtList<*> -> toBaseKotlin()
    is NbtCompound -> toBaseKotlin()
    else -> throw AssertionError()
}

fun NbtByte.toBaseKotlin() = value
fun NbtShort.toBaseKotlin() = value
fun NbtInt.toBaseKotlin() = value
fun NbtLong.toBaseKotlin() = value
fun NbtFloat.toBaseKotlin() = value
fun NbtDouble.toBaseKotlin() = value
fun NbtString.toBaseKotlin() = value

fun NbtByteArray.toBaseKotlin() = ByteArray(size) { this[it] }
fun NbtIntArray.toBaseKotlin() = IntArray(size) { this[it] }
fun NbtLongArray.toBaseKotlin() = LongArray(size) { this[it] }

fun <T : NbtTag> NbtList<T>.toBaseKotlin() = mapTo(mutableListOf()) { it.toBaseKotlin() }
// region Type-safe list converters
@Suppress("UNCHECKED_CAST")
@JvmName("toBaseKotlinByteList")
fun NbtList<NbtByte>.toBaseKotlin() = (this as NbtList<*>).toBaseKotlin() as MutableList<Byte>
@Suppress("UNCHECKED_CAST")
@JvmName("toBaseKotlinShortList")
fun NbtList<NbtShort>.toBaseKotlin() = (this as NbtList<*>).toBaseKotlin() as MutableList<Short>
@Suppress("UNCHECKED_CAST")
@JvmName("toBaseKotlinIntList")
fun NbtList<NbtInt>.toBaseKotlin() = (this as NbtList<*>).toBaseKotlin() as MutableList<Int>
@Suppress("UNCHECKED_CAST")
@JvmName("toBaseKotlinLongList")
fun NbtList<NbtLong>.toBaseKotlin() = (this as NbtList<*>).toBaseKotlin() as MutableList<Long>
@Suppress("UNCHECKED_CAST")
@JvmName("toBaseKotlinFloatList")
fun NbtList<NbtFloat>.toBaseKotlin() = (this as NbtList<*>).toBaseKotlin() as MutableList<Float>
@Suppress("UNCHECKED_CAST")
@JvmName("toBaseKotlinDoubleList")
fun NbtList<NbtDouble>.toBaseKotlin() = (this as NbtList<*>).toBaseKotlin() as MutableList<Double>
@Suppress("UNCHECKED_CAST")
@JvmName("toBaseKotlinByteArrayList")
fun NbtList<NbtByteArray>.toBaseKotlin() = (this as NbtList<*>).toBaseKotlin() as MutableList<ByteArray>
@Suppress("UNCHECKED_CAST")
@JvmName("toBaseKotlinStringList")
fun NbtList<NbtString>.toBaseKotlin() = (this as NbtList<*>).toBaseKotlin() as MutableList<String>
@Suppress("UNCHECKED_CAST")
@JvmName("toBaseKotlinListList")
fun NbtList<NbtList<*>>.toBaseKotlin() = (this as NbtList<*>).toBaseKotlin() as MutableList<MutableList<*>>
@Suppress("UNCHECKED_CAST")
@JvmName("toBaseKotlinCompoundList")
fun NbtList<NbtCompound>.toBaseKotlin() = (this as NbtList<*>).toBaseKotlin() as MutableList<Map<String, *>>
@Suppress("UNCHECKED_CAST")
@JvmName("toBaseKotlinIntArrayList")
fun NbtList<NbtIntArray>.toBaseKotlin() = (this as NbtList<*>).toBaseKotlin() as MutableList<IntArray>
@Suppress("UNCHECKED_CAST")
@JvmName("toBaseKotlinLongArrayList")
fun NbtList<NbtLongArray>.toBaseKotlin() = (this as NbtList<*>).toBaseKotlin() as MutableList<LongArray>
// endregion

fun NbtCompound.toBaseKotlin() = asSequence().associateTo(mutableMapOf()) { (k, v) -> k to v.toBaseKotlin() }
