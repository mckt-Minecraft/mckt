package io.github.gaming32.mckt.objects

import kotlinx.serialization.Serializable

@Serializable(with = IdentifierSerializer::class)
data class Identifier(val namespace: String, val value: String) {
    companion object {
        fun parse(s: String) = s.indexOf(':').let { colonIndex ->
            if (colonIndex == -1) Identifier(s)
            else Identifier(s.substring(0, colonIndex), s.substring(colonIndex + 1))
        }
    }

    init {
        if (namespace != "minecraft") {
            for (c in namespace) {
                if (
                    (c in 'a'..'z') ||
                    (c in '0'..'9') ||
                    c == '.' || c == '-' || c == '_'
                ) continue
                throw IllegalArgumentException("Invalid namespace $namespace")
            }
        }
        for (c in value) {
            if (
                (c in 'a'..'z') ||
                (c in '0'..'9') ||
                c == '.' || c == '-' || c == '_' || c == '/'
            ) continue
            throw IllegalArgumentException("Invalid value $value")
        }
    }

    constructor(value: String) : this("minecraft", value)

    override fun toString() = "$namespace:$value"
}
