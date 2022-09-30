package io.github.gaming32.mckt.commands.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import io.github.gaming32.mckt.commands.CommandSource
import io.github.gaming32.mckt.commands.EntitySelector
import io.github.gaming32.mckt.commands.arguments.getPlayers
import io.github.gaming32.mckt.commands.arguments.players
import io.github.gaming32.mckt.commands.executesSuspend
import net.kyori.adventure.text.Component

object DeopCommand : BuiltinCommand {
    override val helpText = Component.text("Sets a player's operator level to 0")

    override fun buildTree() = literal<CommandSource>("deop")
        .requires { it.hasPermission(3) }
        .then(argument<CommandSource, EntitySelector>("player", players())
            .executesSuspend {
                getPlayers("player").forEach { player ->
                    player.setOperatorLevel(0)
                    source.replyBroadcast(Component.translatable(
                        "commands.deop.success",
                        Component.text(player.username)
                    ))
                }
                0
            }
        )!!
}
