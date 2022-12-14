package io.github.gaming32.mckt.util

import it.unimi.dsi.fastutil.ints.IntIntPair
import it.unimi.dsi.fastutil.longs.Long2ObjectMap
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap

class IntIntPair2ObjectMap<V>(private val internal: Long2ObjectMap<V> = Long2ObjectOpenHashMap()) {
    private fun join(x: Int, y: Int) = x.toLong() shl 32 or y.toUInt().toLong()

    val values: Collection<V> get() = internal.values

    operator fun get(x: Int, y: Int): V? = internal[join(x, y)]

    operator fun get(key: IntIntPair) = this[key.firstInt(), key.secondInt()]

    operator fun set(x: Int, y: Int, value: V) {
        internal.put(join(x, y), value)
    }

    operator fun set(key: IntIntPair, value: V) {
        this[key.firstInt(), key.secondInt()] = value
    }

    fun contains(x: Int, y: Int) = internal.containsKey(join(x, y))

    operator fun contains(key: IntIntPair) = contains(key.firstInt(), key.secondInt())

    fun clear() = internal.clear()
}
