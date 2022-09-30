package io.github.gaming32.mckt.commands.commands

import com.mojang.brigadier.arguments.StringArgumentType.greedyString
import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import io.github.gaming32.mckt.commands.CommandSource
import io.github.gaming32.mckt.commands.arguments.getString
import io.github.gaming32.mckt.commands.executesSuspend
import net.kyori.adventure.text.Component

object SayCommand : BuiltinCommand {
    override val helpText = Component.text("Send a message")

    override fun buildTree() = literal<CommandSource>("say")
        .then(argument<CommandSource, String>("message", greedyString())
            .executesSuspend {
                val message = getString("message")
                source.server.broadcastChat(
                    Component.translatable("chat.type.announcement", source.displayName, Component.text(message))
                )
                0
            }
        )!!
}
