package io.github.gaming32.mckt.util

import it.unimi.dsi.fastutil.ints.IntIntImmutablePair
import it.unimi.dsi.fastutil.ints.IntIntPair
import it.unimi.dsi.fastutil.longs.Long2ObjectFunction
import it.unimi.dsi.fastutil.longs.Long2ObjectMap
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap

class IntInt2ObjectBiMap<V>(
    @PublishedApi internal val internal: Long2ObjectMap<V> = Long2ObjectOpenHashMap()
) : Iterable<Pair<IntIntPair, V>> {
    @PublishedApi internal fun join(x: Int, y: Int) = x.toLong() shl 32 or y.toLong()
    private fun split(v: Long): IntIntPair = IntIntImmutablePair((v ushr 32).toInt(), (v and 0xffffffffL).toInt())

    val values = internal.values

    operator fun get(x: Int, y: Int): V? = internal[join(x, y)]
    operator fun set(x: Int, y: Int, value: V) {
        internal[join(x, y)] = value
    }

    inline fun computeIfAbsent(x: Int, y: Int, crossinline compute: (Int, Int) -> V): V =
        internal.computeIfAbsent(join(x, y), Long2ObjectFunction { compute(x, y) })

    override fun iterator(): Iterator<Pair<IntIntPair, V>> =
        internal.long2ObjectEntrySet()
            .asSequence()
            .map { split(it.longKey) to it.value }
            .iterator()

    fun clear() = internal.clear()
}
