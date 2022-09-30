package io.github.gaming32.mckt.commands.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import com.mojang.brigadier.context.CommandContext
import io.github.gaming32.mckt.commands.CommandSource
import io.github.gaming32.mckt.commands.EntitySelector
import io.github.gaming32.mckt.commands.arguments.TextArgumentType
import io.github.gaming32.mckt.commands.arguments.TextArgumentType.getTextComponent
import io.github.gaming32.mckt.commands.arguments.getPlayers
import io.github.gaming32.mckt.commands.arguments.players
import io.github.gaming32.mckt.commands.executesSuspend
import net.kyori.adventure.text.Component

object KickCommand : BuiltinCommand {
    override val helpText = Component.text("Forcefully disconnect a player")

    override fun buildTree() = literal<CommandSource>("kick")
        .requires { it.hasPermission(2) }
        .then(argument<CommandSource, EntitySelector>("player", players())
            .executesSuspend { kick(Component.translatable("multiplayer.disconnect.kicked")) }
            .then(argument<CommandSource, Component>("reason", TextArgumentType)
                .executesSuspend { kick(getTextComponent("reason")) }
            )
        )!!

    private suspend fun CommandContext<CommandSource>.kick(reason: Component): Int {
        getPlayers("player").forEach { player ->
            player.kick(reason)
            source.replyBroadcast(Component.translatable(
                "commands.kick.success",
                Component.text(player.username),
                reason
            ))
        }
        return 0
    }
}
