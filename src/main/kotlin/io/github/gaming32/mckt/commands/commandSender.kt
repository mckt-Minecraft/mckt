package io.github.gaming32.mckt.commands

import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import io.github.gaming32.mckt.MinecraftServer
import io.github.gaming32.mckt.PlayClient
import io.github.gaming32.mckt.getLogger
import io.github.gaming32.mckt.packet.play.s2c.SystemChatPacket
import io.github.gaming32.mckt.plainText
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

private val LOGGER = getLogger()

abstract class CommandSender(val server: MinecraftServer) {
    abstract val displayName: Component
    abstract val operator: Int

    abstract suspend fun reply(message: Component)

    fun getCompletions(ctx: CommandContext<CommandSender>) = Suggestions.empty()

    suspend fun replyBroadcast(message: Component) {
        reply(message)
        val broadcastMessage = Component.text {
            it.color(NamedTextColor.GRAY)
            it.append(Component.text('['))
            it.append(displayName)
            it.append(Component.text(": "))
            it.append(message)
            it.append(Component.text(']'))
        }
        if (this !is ConsoleCommandSender) {
            LOGGER.info(broadcastMessage.plainText())
        }
        val skipClient = (this as? ClientCommandSender)?.client
        server.broadcast(SystemChatPacket(broadcastMessage)) { client ->
            client.data.operatorLevel > 0 && client != skipClient
        }
    }

    override fun toString() = displayName.plainText()
}

class ConsoleCommandSender(server: MinecraftServer, name: String) : CommandSender(server) {
    override val displayName = Component.text(name)
    override val operator = 4

    override suspend fun reply(message: Component) = LOGGER.info(message.plainText())
}

class ClientCommandSender(val client: PlayClient) : CommandSender(client.server) {
    override val displayName = Component.text(client.username)
    override val operator get() = client.data.operatorLevel

    override suspend fun reply(message: Component) = client.sendPacket(SystemChatPacket(message))
}
