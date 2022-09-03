@file:UseSerializers(TextSerializer::class)

package io.github.gaming32.mckt

import io.github.gaming32.mckt.objects.TextSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import net.kyori.adventure.text.Component

@Serializable
class ServerConfig(
    var viewDistance: Int = 10,
    var simulationDistance: Int = 10,
    var maxPlayers: Int = 20,
    var motd: Component = Component.text("My mckt server")
)
