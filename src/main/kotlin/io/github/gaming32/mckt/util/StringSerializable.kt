package io.github.gaming32.mckt.util

interface StringSerializable {
    object EmptySerializable : StringSerializable {
        override fun serializeToString() = ""
    }

    fun serializeToString(): String
}
