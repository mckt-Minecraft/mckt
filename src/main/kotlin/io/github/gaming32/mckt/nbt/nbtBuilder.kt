package io.github.gaming32.mckt.nbt

import net.kyori.adventure.nbt.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.experimental.ExperimentalTypeInference

@DslMarker
internal annotation class NbtDslMarker

//region NbtListBuilder
@NbtDslMarker
class NbtListBuilder<T : NbtListElement<T>> @PublishedApi internal constructor(size: Int = -1) {
    private val elements by lazy {
        if (size >= 0) ArrayList<T>(size) else ArrayList<T>()
    }

    private var elementType: BinaryTagType<*> = BinaryTagTypes.END
    private var built = false

    @PublishedApi
    internal fun add(tag: T): Boolean {
        if (built) throw UnsupportedOperationException("List has already been built")

        if (elementType == BinaryTagTypes.END) {
            elementType = tag.type
        } else {
            require(tag.type == elementType) { "Cannot add a ${tag.type} to a list of $elementType" }
        }

        elements.add(tag)
        return true
    }

    @PublishedApi
    internal fun build(): NbtList<T> {
        built = true
        return NbtList(elements)
    }
}

@OptIn(ExperimentalContracts::class)
inline fun <T : NbtListElement<T>> buildNbtList(
    builderAction: NbtListBuilder<T>.() -> Unit,
): NbtList<T> {
    contract {
        callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE)
    }
    return NbtListBuilder<T>().apply(builderAction).build()
}

fun NbtListBuilder<NbtByte>.add(tag: NbtByte): Boolean = add(tag)
fun NbtListBuilder<NbtByteArray>.add(tag: NbtByteArray): Boolean = add(tag)
fun NbtListBuilder<NbtCompound>.add(tag: NbtCompound): Boolean = add(tag)
fun NbtListBuilder<NbtDouble>.add(tag: NbtDouble): Boolean = add(tag)
fun NbtListBuilder<NbtFloat>.add(tag: NbtFloat): Boolean = add(tag)
fun NbtListBuilder<NbtInt>.add(tag: NbtInt): Boolean = add(tag)
fun NbtListBuilder<NbtIntArray>.add(tag: NbtIntArray): Boolean = add(tag)
fun <T : NbtListElement<T>> NbtListBuilder<NbtList<T>>.add(tag: NbtList<T>): Boolean = add(tag)
fun NbtListBuilder<NbtLong>.add(tag: NbtLong): Boolean = add(tag)
fun NbtListBuilder<NbtLongArray>.add(tag: NbtLongArray): Boolean = add(tag)
fun NbtListBuilder<NbtShort>.add(tag: NbtShort): Boolean = add(tag)
fun NbtListBuilder<NbtString>.add(tag: NbtString): Boolean = add(tag)

fun NbtListBuilder<NbtByte>.add(value: Byte): Boolean = add(NbtByte(value))
fun NbtListBuilder<NbtByte>.add(value: Boolean): Boolean = add(NbtByte(value))
fun NbtListBuilder<NbtShort>.add(value: Short): Boolean = add(NbtShort(value))
fun NbtListBuilder<NbtInt>.add(value: Int): Boolean = add(NbtInt(value))
fun NbtListBuilder<NbtLong>.add(value: Long): Boolean = add(NbtLong(value))
fun NbtListBuilder<NbtFloat>.add(value: Float): Boolean = add(NbtFloat(value))
fun NbtListBuilder<NbtDouble>.add(value: Double): Boolean = add(NbtDouble(value))
fun NbtListBuilder<NbtByteArray>.add(value: ByteArray): Boolean = add(NbtByteArray(value))
fun NbtListBuilder<NbtString>.add(value: String): Boolean = add(NbtString(value))
fun NbtListBuilder<NbtIntArray>.add(value: IntArray): Boolean = add(NbtIntArray(value))
fun NbtListBuilder<NbtLongArray>.add(value: LongArray): Boolean = add(NbtLongArray(value))

@OptIn(ExperimentalTypeInference::class, ExperimentalContracts::class)
inline fun <T : NbtListElement<T>> NbtListBuilder<NbtList<*>>.addNbtList(
    @BuilderInference builderAction: NbtListBuilder<T>.() -> Unit,
): Boolean {
    contract {
        callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE)
    }
    return add(buildNbtList(builderAction))
}

@OptIn(ExperimentalContracts::class)
inline fun NbtListBuilder<NbtCompound>.addNbtCompound(
    builderAction: NbtCompoundBuilder.() -> Unit,
): Boolean {
    contract {
        callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE)
    }
    return add(buildNbtCompound(builderAction))
}
//endregion

//region NbtCompoundBuilder
@NbtDslMarker
class NbtCompoundBuilder @PublishedApi internal constructor() {
    private val tags by lazy { LinkedHashMap<String, NbtElement<*, *>>() }
    private var empty = true
    private var built = false

    fun put(key: String, tag: NbtElement<*, *>): NbtElement<*, *>? {
        if (built) throw UnsupportedOperationException("Compound has already been built")
        empty = false
        return tags.put(key, tag)
    }

    @PublishedApi
    internal fun build(): NbtCompound {
        built = true
        return NbtCompound(tags)
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildNbtCompound(builderAction: NbtCompoundBuilder.() -> Unit): NbtCompound {
    contract {
        callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE)
    }
    return NbtCompoundBuilder().apply(builderAction).build()
}

fun NbtCompoundBuilder.put(key: String, value: Byte): NbtElement<*, *>? = put(key, NbtByte(value))
fun NbtCompoundBuilder.put(key: String, value: Boolean): NbtElement<*, *>? = put(key, NbtByte(value))
fun NbtCompoundBuilder.put(key: String, value: Short): NbtElement<*, *>? = put(key, NbtShort(value))
fun NbtCompoundBuilder.put(key: String, value: Int): NbtElement<*, *>? = put(key, NbtInt(value))
fun NbtCompoundBuilder.put(key: String, value: Long): NbtElement<*, *>? = put(key, NbtLong(value))
fun NbtCompoundBuilder.put(key: String, value: Float): NbtElement<*, *>? = put(key, NbtFloat(value))
fun NbtCompoundBuilder.put(key: String, value: Double): NbtElement<*, *>? = put(key, NbtDouble(value))
fun NbtCompoundBuilder.put(key: String, value: ByteArray): NbtElement<*, *>? = put(key, NbtByteArray(value))
fun NbtCompoundBuilder.put(key: String, value: String): NbtElement<*, *>? = put(key, NbtString(value))
fun NbtCompoundBuilder.put(key: String, value: IntArray): NbtElement<*, *>? = put(key, NbtIntArray(value))
fun NbtCompoundBuilder.put(key: String, value: LongArray): NbtElement<*, *>? = put(key, NbtLongArray(value))

@OptIn(ExperimentalContracts::class)
inline fun <T : NbtListElement<T>> NbtCompoundBuilder.putNbtList(
    key: String,
    builderAction: NbtListBuilder<T>.() -> Unit,
): NbtElement<*, *>? {
    contract {
        callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE)
    }
    return put(key, buildNbtList(builderAction))
}

@OptIn(ExperimentalContracts::class)
inline fun NbtCompoundBuilder.putNbtCompound(
    key: String,
    builderAction: NbtCompoundBuilder.() -> Unit,
): NbtElement<*, *>? {
    contract {
        callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE)
    }
    return put(key, buildNbtCompound(builderAction))
}
//endregion
