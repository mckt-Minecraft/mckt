package io.github.gaming32.mckt.util

import it.unimi.dsi.fastutil.ints.Int2IntArrayMap
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap

class PalettedStorage(val size: Int) {
    private var palette = Int2IntBiMap(::Int2IntArrayMap).apply {
        keyToValueDefaultReturnValue = -1
        valueToKeyDefaultReturnValue = 0
    }
    private var storage = SimpleBitStorage(4, size)
    private val paletteCapacity get() = 1 shl storage.bits

    operator fun get(index: Int) = palette.getKey(storage[index])
    operator fun set(index: Int, value: Int) {
        var paletteIndex = palette.getValue(value)
        if (paletteIndex != -1) {
            // FAST PATH: Item already in palette
            storage[index] = paletteIndex
            return
        }
        paletteIndex = palette.size
        if (paletteIndex >= paletteCapacity) {
            // SLOW PATH: Palette needs resize
            val newBits = storage.bits + 1
            if (newBits == 5) {
                palette = palette.copyOf(::Int2IntOpenHashMap)
            }
            val newStorage = SimpleBitStorage(newBits, size)
            repeat(size) { i ->
                newStorage[i] = storage[i]
            }
        }
        palette[paletteIndex] = value
        storage[index] = paletteIndex
    }
}
