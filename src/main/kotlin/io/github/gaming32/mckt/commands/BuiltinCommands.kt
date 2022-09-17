package io.github.gaming32.mckt.commands

import com.google.gson.JsonSyntaxException
import io.github.gaming32.mckt.packet.play.s2c.PlayDisconnectPacket
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer

object BuiltinCommands {
    val TELEPORT = registerCommand("tp", Component.text("Teleport a player"), 1) { sender, args ->
        val argv = args.split(" ")
        if (argv.size < 2) {
            return@registerCommand sender.reply(
                Component.text("Usage:\n  /tp <who> <to>\n  /tp <who> <x> <y> <z>", NamedTextColor.RED)
            )
        }
        val who = sender.evaluateClient(argv[0]) ?: return@registerCommand sender.reply(
            Component.text("Player ${argv[0]} not found", NamedTextColor.RED)
        )
        if (argv.size < 4) {
            val to = sender.evaluateClient(argv[1]) ?: return@registerCommand sender.reply(
                Component.text("Player ${argv[1]} not found", NamedTextColor.RED)
            )
            who.teleport(to)
            sender.replyBroadcast(Component.translatable(
                "commands.teleport.success.entity.single",
                Component.text(who.username),
                Component.text(to.username)
            ))
        } else {
            val x = argv[1].toDoubleOrNull() ?: return@registerCommand sender.reply(
                Component.text("Invalid number: ${argv[1]}", NamedTextColor.RED)
            )
            val y = argv[2].toDoubleOrNull() ?: return@registerCommand sender.reply(
                Component.text("Invalid number: ${argv[2]}", NamedTextColor.RED)
            )
            val z = argv[3].toDoubleOrNull() ?: return@registerCommand sender.reply(
                Component.text("Invalid number: ${argv[3]}", NamedTextColor.RED)
            )
            who.teleport(x, y, z)
            sender.replyBroadcast(Component.text("Teleported ${who.username} to $x $y $z"))
            sender.replyBroadcast(Component.translatable(
                "commands.teleport.success.location.single",
                Component.text(who.username),
                Component.text(x), Component.text(y), Component.text(z)
            ))
        }
    }

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
