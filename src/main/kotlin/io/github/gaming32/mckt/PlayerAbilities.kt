package io.github.gaming32.mckt

import kotlinx.serialization.Serializable

@Serializable
data class PlayerAbilities(
    val invulnerable: Boolean,
    val flying: Boolean,
    val allowFlying: Boolean,
    val creativeMode: Boolean,
    val flySpeed: Float = 0.05f,
    val walkSpeed: Float = 0.1f
) {
    fun copyCurrentlyFlying(currentlyFlying: Boolean): PlayerAbilities {
        if (allowFlying && currentlyFlying) {
            if (!flying) return copy(flying = true)
        } else {
            if (flying) return copy(flying = false)
        }
        return this
    }
}
