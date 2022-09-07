package io.github.gaming32.mckt

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Gamemode(val defaultAbilities: PlayerAbilities) {
    @SerialName("survival")  SURVIVAL(PlayerAbilities(
        invulnerable = false,
        flying = false,
        allowFlying = false,
        creativeMode = false
    )),
    @SerialName("creative")  CREATIVE(PlayerAbilities(
        invulnerable = true,
        flying = false,
        allowFlying = true,
        creativeMode = true
    )),
    @SerialName("adventure") ADVENTURE(SURVIVAL.defaultAbilities),
    @SerialName("spectator") SPECTATOR(CREATIVE.defaultAbilities.copy(flying = true, creativeMode = false))
}
