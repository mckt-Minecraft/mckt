package io.github.gaming32.mckt.nbt

import net.kyori.adventure.nbt.*

fun BinaryTag.toNbt(): NbtElement<*, *> = when (this) {
    is ByteBinaryTag -> toNbt()
    is ShortBinaryTag -> toNbt()
    is IntBinaryTag -> toNbt()
    is LongBinaryTag -> toNbt()
    is FloatBinaryTag -> toNbt()
    is DoubleBinaryTag -> toNbt()
    is ByteArrayBinaryTag -> toNbt()
    is StringBinaryTag -> toNbt()
    is ListBinaryTag -> toNbt()
    is CompoundBinaryTag -> toNbt()
    is IntArrayBinaryTag -> toNbt()
    is LongArrayBinaryTag -> toNbt()
    else -> throw AssertionError()
}

fun ByteBinaryTag.toNbt() = NbtByte(value())
fun ShortBinaryTag.toNbt() = NbtShort(value())
fun IntBinaryTag.toNbt() = NbtInt(value())
fun LongBinaryTag.toNbt() = NbtLong(value())
fun FloatBinaryTag.toNbt() = NbtFloat(value())
fun DoubleBinaryTag.toNbt() = NbtDouble(value())

fun ByteArrayBinaryTag.toNbt() = NbtByteArray(value())

fun StringBinaryTag.toNbt() = NbtString(value())

fun ListBinaryTag.toNbt() = NbtList(mapTo(mutableListOf()) { it.toNbt() })

fun CompoundBinaryTag.toNbt() = NbtCompound(asSequence()
    .associateTo(mutableMapOf()) { (key, value) ->
        key to value.toNbt()
    }
)

fun IntArrayBinaryTag.toNbt() = NbtIntArray(value())
fun LongArrayBinaryTag.toNbt() = NbtLongArray(value())
