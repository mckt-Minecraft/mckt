package io.github.gaming32.mckt.util

import it.unimi.dsi.fastutil.ints.Int2IntMap

class Int2IntBiMap(mapFactory: () -> Int2IntMap) {
    private val keyToValue = mapFactory()
    private val valueToKey = mapFactory()
    val size get() = keyToValue.size

    var keyToValueDefaultReturnValue: Int
        get() = keyToValue.defaultReturnValue()
        set(rv) {
            keyToValue.defaultReturnValue(rv)
        }
    var valueToKeyDefaultReturnValue: Int
        get() = valueToKey.defaultReturnValue()
        set(rv) {
            valueToKey.defaultReturnValue(rv)
        }

    fun getValue(key: Int) = keyToValue[key]
    fun getKey(value: Int) = valueToKey[value]

    operator fun set(key: Int, value: Int) {
        keyToValue.put(key, value)
        valueToKey.put(value, key)
    }

    fun clear() {
        keyToValue.clear()
        valueToKey.clear()
    }

    fun removeKey(key: Int): Int {
        val oldValue = keyToValue.remove(key)
        valueToKey.remove(oldValue)
        return oldValue
    }

    fun removeValue(value: Int): Int {
        val oldKey = valueToKey.remove(value)
        keyToValue.remove(oldKey)
        return oldKey
    }

    fun remove(key: Int, value: Int) {
        val oldValue = keyToValue.get(key)
        if (value != oldValue) return
        keyToValue.remove(key)
        valueToKey.remove(value)
    }

    fun copyOf(mapFactory: () -> Int2IntMap) = Int2IntBiMap(mapFactory).also { new ->
        new.keyToValueDefaultReturnValue = keyToValueDefaultReturnValue
        new.valueToKeyDefaultReturnValue = valueToKeyDefaultReturnValue
        new.keyToValue.putAll(keyToValue)
        new.valueToKey.putAll(valueToKey)
    }
}
