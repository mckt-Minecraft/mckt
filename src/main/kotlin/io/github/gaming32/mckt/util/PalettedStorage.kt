package io.github.gaming32.mckt.util

import io.github.gaming32.mckt.data.writeByte
import io.github.gaming32.mckt.data.writeLong
import io.github.gaming32.mckt.data.writeVarInt
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import java.io.OutputStream

class PalettedStorage<V>(
    val size: Int,
    private val defaultValue: V,
    private val encoder: OutputStream.(V) -> Unit
) {
    private var palette = Int2ObjectBiMap<V>(::Int2ObjectArrayMap, ::Object2IntArrayMap).apply {
        valueToKeyDefaultReturnValue = -1
    }
    internal var storage = SimpleBitStorage(4, size)
    private val paletteCapacity get() = 1 shl storage.bits

    init {
        palette[0] = defaultValue
    }

    operator fun get(index: Int) = palette.getValue(storage[index]) ?: defaultValue
    operator fun set(index: Int, value: V) {
        var paletteIndex = palette.getKey(value)
        if (paletteIndex != -1) {
            // FAST PATH: Item already in palette
            storage[index] = paletteIndex
            return
        }
        paletteIndex = palette.size
        if (paletteIndex >= paletteCapacity) {
            // SLOW PATH: Palette needs resize
            val newBits = storage.bits + 1
            if (newBits == 5) expandPalette()
            val newStorage = SimpleBitStorage(newBits, size)
            repeat(size) { i ->
                newStorage[i] = storage[i]
            }
            storage = newStorage
        }
        palette[paletteIndex] = value
        storage[index] = paletteIndex
    }

    fun getPaletteItems() = (0 until palette.size).asSequence().map { palette.getValue(it) }

    fun encode(out: OutputStream) {
        out.writeByte(storage.bits)
        out.writeVarInt(palette.size)
        palette.values.forEach { out.encoder(it) }
        val longs = storage.data
        out.writeVarInt(longs.size)
        longs.forEach { out.writeLong(it) }
    }

    private fun expandPalette() {
        palette = palette.copyOf(::Int2ObjectOpenHashMap, ::Object2IntOpenHashMap)
    }

    internal fun setPaletteItems(items: List<V>) {
        palette.clear()
        if (items.size > 16) {
            expandPalette()
        }
        items.forEachIndexed { index, value ->
            palette[index] = value
        }
    }

    internal fun compact() {
        val new = PalettedStorage(size, defaultValue, encoder)
        repeat(size) { i ->
            new[i] = this[i]
        }
        if (new.palette.size != palette.size) {
            palette = new.palette
            storage = new.storage
        }
    }
}
