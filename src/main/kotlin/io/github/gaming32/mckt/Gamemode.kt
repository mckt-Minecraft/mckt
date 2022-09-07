package io.github.gaming32.mckt

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Gamemode {
    @SerialName("survival")  SURVIVAL,
    @SerialName("creative")  CREATIVE,
    @SerialName("adventure") ADVENTURE,
    @SerialName("spectator") SPECTATOR
}
