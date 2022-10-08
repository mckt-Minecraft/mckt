@file:UseSerializers(TextSerializer::class)

package io.github.gaming32.mckt

import io.github.gaming32.mckt.objects.TextSerializer
import kotlinx.serialization.*
import net.kyori.adventure.text.Component

@Serializable
class ServerConfig(
    var viewDistance: Int = 10,
    var simulationDistance: Int = 10,
    var maxPlayers: Int = 20,
    var seed: Long? = null,
    var motd: Component = Component.text("My mckt server"),
    var defaultWorldGenerator: WorldGenerator = WorldGenerator.NORMAL,
    var networkCompressionThreshold: Int = 256,
    var autosavePeriod: Int = 6000,
    var enableVanillaClientSpoofAlerts: Boolean = true
)
