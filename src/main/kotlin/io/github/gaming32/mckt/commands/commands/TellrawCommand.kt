package io.github.gaming32.mckt.commands.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import io.github.gaming32.mckt.commands.ClientCommandSource
import io.github.gaming32.mckt.commands.CommandSource
import io.github.gaming32.mckt.commands.EntitySelector
import io.github.gaming32.mckt.commands.arguments.TextArgumentType
import io.github.gaming32.mckt.commands.arguments.TextArgumentType.getTextComponent
import io.github.gaming32.mckt.commands.arguments.getPlayers
import io.github.gaming32.mckt.commands.arguments.players
import io.github.gaming32.mckt.commands.executesSuspend
import net.kyori.adventure.text.Component

object TellrawCommand : BuiltinCommand {
    override val helpText = Component.text("Send a custom message")

    override fun buildTree() = literal<CommandSource>("tellraw")
        .requires { it.hasPermission(1) }
        .then(argument<CommandSource, EntitySelector>("targets", players())
            .then(argument<CommandSource, Component>("message", TextArgumentType)
                .executesSuspend {
                    val targets = getPlayers("targets")
                    val message = getTextComponent("message")
                    targets.forEach { it.sendMessage(message) }
                    if (source !is ClientCommandSource) {
                        source.reply(Component.text()
                            .append(Component.text("Sent raw message \""))
                            .append(message)
                            .append(Component.text("\" to ${targets.size} player(s)"))
                            .build()
                        )
                    }
                    0
                }
            )
        )!!
}
