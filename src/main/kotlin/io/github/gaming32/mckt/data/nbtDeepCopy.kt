package io.github.gaming32.mckt.data

import net.benwoodworth.knbt.*

@Suppress("UNCHECKED_CAST")
fun <T : NbtTag> T.deepCopy(): T = when (this) {
    is NbtByteArray -> deepCopy() as T
    is NbtList<*> -> deepCopy() as T
    is NbtCompound -> deepCopy() as T
    is NbtIntArray -> deepCopy() as T
    is NbtLongArray -> deepCopy() as T
    else -> this
}

fun NbtByteArray.deepCopy() = NbtByteArray(toByteArray())

// We're abusing unchecked casts here. Trust me, the alternative requires separate implementations for each type, and
// requires deciding at runtime which one to use, despite the same end result.
@Suppress("UNCHECKED_CAST")
fun <T : NbtTag> NbtList<T>.deepCopy() = NbtList(this as NbtList<NbtByte>) as NbtList<T>

fun NbtCompound.deepCopy() = NbtCompound(entries.associate { (key, value) -> key to value.deepCopy() })

fun NbtIntArray.deepCopy() = NbtIntArray(toIntArray())

fun NbtLongArray.deepCopy() = NbtLongArray(toLongArray())

// Primitives
fun NbtByte.deepCopy() = this
fun NbtShort.deepCopy() = this
fun NbtInt.deepCopy() = this
fun NbtLong.deepCopy() = this
fun NbtFloat.deepCopy() = this
fun NbtDouble.deepCopy() = this
fun NbtString.deepCopy() = this
