package io.github.gaming32.mckt.commands

import com.google.gson.JsonSyntaxException
import io.github.gaming32.mckt.packet.play.s2c.PlayDisconnectPacket
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer

object BuiltinCommands {
    val KICK = registerCommand("kick", Component.text("Forcefully disconnect a player"), 2) { sender, args ->
        val spaceIndex = args.indexOf(' ')
        val (username, reason) = if (spaceIndex != -1) {
            Pair(
                args.substring(0, spaceIndex),
                try {
                    GsonComponentSerializer.gson().deserialize(args.substring(spaceIndex + 1))
                } catch (e: JsonSyntaxException) {
                    Component.text(args.substring(spaceIndex + 1))
                }
            )
        } else {
            Pair(args, Component.translatable("multiplayer.disconnect.kicked"))
        }
        val client = sender.evaluateClient(username)
        if (client == null || client.receiveChannel.isClosedForRead) {
            return@registerCommand sender.reply(Component.text("Player $username is not online.", NamedTextColor.RED))
        }
        client.sendPacket(PlayDisconnectPacket(reason))
        client.socket.dispose()
        sender.replyBroadcast(Component.text("Kicked $username for ").append(reason))
        sender.replyBroadcast(Component.translatable("commands.kick.success", Component.text(username), reason))
    }

    internal fun register() = Unit
}
