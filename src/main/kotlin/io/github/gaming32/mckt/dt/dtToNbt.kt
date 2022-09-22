package io.github.gaming32.mckt.dt

import net.benwoodworth.knbt.*

fun NbtTag.toDt(): DtElement<*, *> = when (this) {
    is NbtByte -> toDt()
    is NbtShort -> toDt()
    is NbtInt -> toDt()
    is NbtLong -> toDt()
    is NbtFloat -> toDt()
    is NbtDouble -> toDt()
    is NbtByteArray -> toDt()
    is NbtString -> toDt()
    is NbtList<*> -> toDt<DtElement<*, *>>()
    is NbtCompound -> toDt()
    is NbtIntArray -> toDt()
    is NbtLongArray -> toDt()
}

fun NbtByte.toDt() = DtByte(value)
fun NbtShort.toDt() = DtShort(value)
fun NbtInt.toDt() = DtInt(value)
fun NbtLong.toDt() = DtLong(value)
fun NbtFloat.toDt() = DtFloat(value)
fun NbtDouble.toDt() = DtDouble(value)

fun NbtByteArray.toDt() = DtByteArray(toByteArray())

fun NbtString.toDt() = DtString(value)

@Suppress("UNCHECKED_CAST") // Somebody has to do it
fun <T : DtElement<*, T>> NbtList<*>.toDt() = DtList(mapTo(mutableListOf()) { it.toDt() }) as DtList<T>

fun NbtCompound.toDt() = DtCompound(asSequence().associateTo(mutableMapOf()) { (key, value) -> key to value.toDt() })

fun NbtIntArray.toDt() = DtIntArray(toIntArray())
fun NbtLongArray.toDt() = DtLongArray(toLongArray())
