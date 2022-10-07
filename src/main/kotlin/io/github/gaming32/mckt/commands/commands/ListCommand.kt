package io.github.gaming32.mckt.commands.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import io.github.gaming32.mckt.commands.CommandSource
import io.github.gaming32.mckt.commands.executesSuspend
import net.kyori.adventure.extra.kotlin.join
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration

object ListCommand : BuiltinCommand {
    override val helpText = Component.text("List the players online")

    override fun buildTree() = literal<CommandSource>("list")
        .executesSuspend {
            val server = source.server
            source.reply(Component.translatable(
                "commands.list.players",
                Component.text(server.clients.size),
                Component.text(server.config.maxPlayers),
                server.clients.keys.map(Component::text).join(JoinConfiguration.commas(true))
            ))
            0
        }!!
}
