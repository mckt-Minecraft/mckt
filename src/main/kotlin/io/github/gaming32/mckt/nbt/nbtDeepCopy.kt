package io.github.gaming32.mckt.nbt

import net.kyori.adventure.nbt.*

@Suppress("UNCHECKED_CAST")
fun <T : BinaryTag> T.deepCopy(): T = when (this) {
    is ByteArrayBinaryTag -> deepCopy() as T
    is ListBinaryTag -> deepCopy() as T
    is CompoundBinaryTag -> deepCopy() as T
    is IntArrayBinaryTag -> deepCopy() as T
    is LongArrayBinaryTag -> deepCopy() as T
    else -> this
}

fun ByteArrayBinaryTag.deepCopy() = ByteArrayBinaryTag.of(*value())

fun ListBinaryTag.deepCopy() = ListBinaryTag.from(this)

fun CompoundBinaryTag.deepCopy() = CompoundBinaryTag.builder().put(this).build()

fun IntArrayBinaryTag.deepCopy() = IntArrayBinaryTag.of(*value())

fun LongArrayBinaryTag.deepCopy() = LongArrayBinaryTag.of(*value())
