package io.github.gaming32.mckt.commands

import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.mojang.brigadier.suggestion.Suggestions
import io.github.gaming32.mckt.*
import io.github.gaming32.mckt.objects.Vector2f
import io.github.gaming32.mckt.objects.Vector3d
import io.github.gaming32.mckt.packet.play.s2c.SystemChatPacket
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor


private val LOGGER = getLogger()

val REQUIRES_PLAYER_EXCEPTION = SimpleCommandExceptionType(
    Component.translatable("permissions.requires.player").wrap()
)
val REQUIRES_ENTITY_EXCEPTION = SimpleCommandExceptionType(
    Component.translatable("permissions.requires.entity").wrap()
)

abstract class CommandSource(val server: MinecraftServer) {
    abstract val displayName: Component
    abstract val operator: Int

    open val position get() = server.world.meta.spawnPos.toVector3d()
    open val rotation get() = Vector2f.ZERO

    open val entity: PlayClient get() = throw REQUIRES_ENTITY_EXCEPTION.create()
    open val player: PlayClient get() = throw REQUIRES_PLAYER_EXCEPTION.create()

    abstract suspend fun reply(message: Component)

    open fun getCompletions(ctx: CommandContext<CommandSource>) = Suggestions.empty()!!

    suspend fun replyBroadcast(message: Component) {
        reply(message)
        val broadcastMessage = Component.translatable("chat.type.admin", NamedTextColor.GRAY, displayName, message)
        if (this !is ConsoleCommandSource) {
            LOGGER.info(if (server.useJline) {
                broadcastMessage.attributedText().toAnsi()
            } else {
                broadcastMessage.plainText()
            })
        }
        val skipClient = (this as? ClientCommandSource)?.client
        server.broadcast(SystemChatPacket(broadcastMessage)) { client ->
            client.data.operatorLevel > 0 && client != skipClient
        }
    }

    fun hasPermission(level: Int) = operator >= level

    override fun toString() = displayName.plainText()
}

class ConsoleCommandSource(server: MinecraftServer, name: String) : CommandSource(server) {
    override val displayName = Component.text(name)
    override val operator get() = 4

    override suspend fun reply(message: Component) {
        val text = if (server.useJline) {
            message.attributedText().toAnsi()
        } else {
            message.plainText()
        }
        if ('\n' in text && text[0] != '\n') {
            LOGGER.info("\n$text")
        } else {
            LOGGER.info(text)
        }
    }
}

class ClientCommandSource(val client: PlayClient) : CommandSource(client.server) {
    override val displayName = Component.text(client.username)
    override val operator get() = client.data.operatorLevel

    override val position get() = Vector3d(client.data.x, client.data.y, client.data.z)
    override val rotation get() = Vector2f(client.data.yaw, client.data.pitch)
    override val entity get() = client
    override val player get() = client

    override suspend fun reply(message: Component) = try {
        client.sendPacket(SystemChatPacket(message))
    } catch (_: Exception) {
        // If the client is disconnected, just swallow the exception (just to be safe)
    }
}
