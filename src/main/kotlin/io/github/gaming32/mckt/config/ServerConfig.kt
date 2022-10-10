package io.github.gaming32.mckt.config

import io.github.gaming32.mckt.WorldGenerator
import net.kyori.adventure.text.Component
import kotlin.script.experimental.annotations.KotlinScript

typealias MotdCreator = suspend MotdCreationContext.() -> Component

typealias ChatFormatter = suspend ChatFormatContext.() -> Component

@KotlinScript(
    fileExtension = "mckt.kts",
    compilationConfiguration = ConfigCompilationConfiguration::class,
    evaluationConfiguration = ConfigEvaluationConfiguration::class
)
abstract class ServerConfig {
    internal object PreConfig : ServerConfig()

    var viewDistance: Int = 10
        protected set

    var simulationDistance: Int = 10
        protected set

    var maxPlayers: Int = 20
        protected set

    var seed: Long? = null
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
        protected set

    private var explicitMotd: Component? = Component.text("My mckt server")

    protected var motd
        get() = explicitMotd ?: throw IllegalStateException("Cannot get explicit motd after setting motd generator")
        set(motd) {
            explicitMotd = motd
            motdGenerator = { explicitMotd!! }
        }

    var motdGenerator: MotdCreator = { explicitMotd!! }
        private set

    protected fun motd(generator: MotdCreator) {
        explicitMotd = null
        motdGenerator = generator
    }

    var chatFormatter: ChatFormatter = { Component.text(message) }
        private set

    protected fun formatChat(formatter: ChatFormatter) {
        chatFormatter = formatter
    }
}
