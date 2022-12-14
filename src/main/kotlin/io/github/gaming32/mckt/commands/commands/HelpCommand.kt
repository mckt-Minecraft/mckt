package io.github.gaming32.mckt.commands.commands

import com.mojang.brigadier.arguments.StringArgumentType.greedyString
import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import io.github.gaming32.mckt.commands.CommandSource
import io.github.gaming32.mckt.commands.arguments.getString
import io.github.gaming32.mckt.commands.executesSuspend
import net.kyori.adventure.extra.kotlin.join
import net.kyori.adventure.extra.kotlin.text
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.format.NamedTextColor

object HelpCommand : BuiltinCommand {
    override val helpText = Component.text("Show this help")

    override fun buildTree() = literal<CommandSource>("help")
        .executesSuspend {
            source.reply(Component.text("Here's a list of the commands you can use:\n")
                .append(source.server.helpTexts.asSequence()
                    .filter { it.key.canUse(source) }
                    .map { (command, description) -> text {
                        append(Component.text("  + /${command.usageText} -- "))
                        if (description != null) {
                            append(description)
                        }
                    } }
                    .toList()
                    .join(JoinConfiguration.newlines())
                )
            )
            0
        }
        .then(argument<CommandSource, String>("command", greedyString())
            .executesSuspend {
                val commandDispatcher = source.server.commandDispatcher
                val commandName = getString("command")
                var result = 0
                source.reply(if (commandName == "all") {
                    commandDispatcher.root.children
                        .asSequence()
                        .filter { it.canUse(source) }
                        .flatMap { command ->
                            commandDispatcher.getAllUsage(command, source, true)
                                .map {
                                    if (it.startsWith("${command.usageText} ->")) {
                                        "/$it" // Command alias
                                    } else {
                                        "/${command.usageText} $it"
                                    }
                                }
                        }
                        .map(Component::text)
                        .toList()
                        .join(JoinConfiguration.newlines())
                } else {
                    val command = commandDispatcher.root.getChild(commandName)
                    if (command == null || !command.canUse(source)) {
                        result = 1
                        Component.translatable(
                            "commands.help.failed",
                            NamedTextColor.RED,
                            Component.text(commandName)
                        )
                    } else {
                        var commandForUsage = command
                        while (commandForUsage.redirect != null) {
                            commandForUsage = commandForUsage.redirect
                        }
                        text {
                            append(commandDispatcher.getAllUsage(commandForUsage, source, true)
                                .map { Component.text("/${command.usageText} $it") }
                                .join(JoinConfiguration.newlines())
                            )
                            source.server.helpTexts[command]?.let { description ->
                                append(Component.newline()).append(description)
                            }
                        }
                    }
                })
                result
            }
            .suggests { ctx, builder ->
                if ("all".startsWith(builder.remainingLowerCase)) {
                    builder.suggest("all")
                }
                ctx.source.server.helpTexts.keys.forEach { node ->
                    if (
                        node.canUse(ctx.source) &&
                        node.usageText.startsWith(builder.remainingLowerCase, ignoreCase = true)
                    ) {
                        builder.suggest(node.usageText)
                    }
                }
                builder.buildFuture()
            }
        )!!
}
