package io.github.gaming32.mckt.util

import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.objects.Object2IntMap

class Int2ObjectBiMap<V>(
    keyToValueMapFactory: () -> Int2ObjectMap<V>,
    valueToKeyMapFactory: () -> Object2IntMap<V>
) {
    private val keyToValue = keyToValueMapFactory()
    private val valueToKey = valueToKeyMapFactory()
    val size get() = keyToValue.size

    var keyToValueDefaultReturnValue: V?
        get() = keyToValue.defaultReturnValue()
        set(rv) {
            keyToValue.defaultReturnValue(rv)
        }
    var valueToKeyDefaultReturnValue: Int
        get() = valueToKey.defaultReturnValue()
        set(rv) {
            valueToKey.defaultReturnValue(rv)
        }

    fun getValue(key: Int): V? = keyToValue[key]
    fun getKey(value: V) = valueToKey.getInt(value)

    operator fun set(key: Int, value: V) {
        keyToValue[key] = value
        valueToKey[value] = key
    }

    val values: Set<V> = valueToKey.keys

    fun clear() {
        keyToValue.clear()
        valueToKey.clear()
    }

    fun removeKey(key: Int): V? {
        val oldValue = keyToValue.remove(key)
        valueToKey.removeInt(oldValue)
        return oldValue
    }

    fun removeValue(value: V): Int {
        val oldKey = valueToKey.removeInt(value)
        keyToValue.remove(oldKey)
        return oldKey
    }

    fun remove(key: Int, value: V) {
        val oldValue = keyToValue.get(key)
        if (value != oldValue) return
        keyToValue.remove(key)
        valueToKey.removeInt(value)
    }

    fun copyOf(
        keyToValueMapFactory: () -> Int2ObjectMap<V>,
        valueToKeyMapFactory: () -> Object2IntMap<V>
    ) = Int2ObjectBiMap(keyToValueMapFactory, valueToKeyMapFactory).also { new ->
        new.keyToValueDefaultReturnValue = keyToValueDefaultReturnValue
        new.valueToKeyDefaultReturnValue = valueToKeyDefaultReturnValue
        new.keyToValue.putAll(keyToValue)
        new.valueToKey.putAll(valueToKey)
    }
}
