package io.github.gaming32.mckt.commands.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import com.mojang.brigadier.context.CommandContext
import io.github.gaming32.mckt.PlayClient
import io.github.gaming32.mckt.commands.CommandSource
import io.github.gaming32.mckt.commands.EntitySelector
import io.github.gaming32.mckt.commands.arguments.getPlayers
import io.github.gaming32.mckt.commands.arguments.players
import io.github.gaming32.mckt.commands.executesSuspend
import net.kyori.adventure.text.Component

object BrandCommand : BuiltinCommand {
    override val helpText = Component.text("View a player's client brand")

    override fun buildTree() = literal<CommandSource>("brand")
        .requires { it.hasPermission(2) }
        .executesSuspend {
            source.reply(Component.text("Your client brand is \"${source.entity.brand}\""))
            supportedChannels(source.entity)
            0
        }
        .then(argument<CommandSource, EntitySelector>("player", players())
            .executesSuspend {
                getPlayers("player").forEach {
                    source.reply(Component.text("${it.username}'s client brand is \"${it.brand}\""))
                    supportedChannels(it)
                }
                0
            }
        )!!

    private suspend fun CommandContext<CommandSource>.supportedChannels(client: PlayClient) {
        if (client.supportedChannels.isEmpty()) return
        source.reply(Component.text("  Supported custom packet channels:"))
        client.supportedChannels.forEach {
            source.reply(Component.text("    + $it"))
        }
    }
}
