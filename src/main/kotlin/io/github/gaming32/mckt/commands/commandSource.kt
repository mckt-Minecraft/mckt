package io.github.gaming32.mckt.commands

import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import io.github.gaming32.mckt.MinecraftServer
import io.github.gaming32.mckt.PlayClient
import io.github.gaming32.mckt.getLogger
import io.github.gaming32.mckt.objects.Vector2f
import io.github.gaming32.mckt.objects.Vector3d
import io.github.gaming32.mckt.packet.play.s2c.SystemChatPacket
import io.github.gaming32.mckt.plainText
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

private val LOGGER = getLogger()

abstract class CommandSource(val server: MinecraftServer) {
    abstract val displayName: Component
    abstract val operator: Int

    open val position get() = server.world.meta.spawnPos.toVector3d()
    open val rotation get() = Vector2f.ZERO

    abstract suspend fun reply(message: Component)

    open fun getCompletions(ctx: CommandContext<CommandSource>) = Suggestions.empty()!!

    suspend fun replyBroadcast(message: Component) {
        reply(message)
        val broadcastMessage = Component.translatable("chat.type.admin", NamedTextColor.GRAY, displayName, message)
        if (this !is ConsoleCommandSource) {
            LOGGER.info(broadcastMessage.plainText())
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

    override suspend fun reply(message: Component) = LOGGER.info(message.plainText())
}

class ClientCommandSource(val client: PlayClient) : CommandSource(client.server) {
    override val displayName = Component.text(client.username)
    override val operator get() = client.data.operatorLevel

    override val position get() = Vector3d(client.data.x, client.data.y, client.data.z)
    override val rotation get() = Vector2f(client.data.yaw, client.data.pitch)

    override suspend fun reply(message: Component) = client.sendPacket(SystemChatPacket(message))
}
