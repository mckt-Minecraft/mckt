package io.github.gaming32.mckt.config

import io.github.gaming32.mckt.MinecraftServer
import io.github.gaming32.mckt.PingInfo
import io.github.gaming32.mckt.PlayClient
import io.github.gaming32.mckt.WorldGenerator
import net.kyori.adventure.text.Component
import kotlin.script.experimental.annotations.KotlinScript

@KotlinScript(
    fileExtension = "mckt.kts",
    compilationConfiguration = ConfigCompilationConfiguration::class,
    evaluationConfiguration = ConfigEvaluationConfiguration::class
)
abstract class ServerConfig {
    var viewDistance: Int = 10
        protected set

    var simulationDistance: Int = 10
        protected set

    var maxPlayers: Int = 20
        protected set

    var seed: Long? = null
        protected set

    var motd: Component = Component.text("My mckt server")
        protected set

    var defaultWorldGenerator: WorldGenerator = WorldGenerator.NORMAL
        protected set

    var networkCompressionThreshold: Int = 256
        protected set

    var autosavePeriod: Int = 5 * 60 * 20
        protected set

    var enableVanillaClientSpoofAlerts: Boolean = true
        protected set

    var enableChatPreview: Boolean = false

    open fun createMotd(server: MinecraftServer, pingInfo: PingInfo): Component = motd

    open fun formatChat(sender: PlayClient, message: String): Component = Component.text(message)
}
