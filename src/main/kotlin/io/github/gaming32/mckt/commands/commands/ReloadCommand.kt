package io.github.gaming32.mckt.commands.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import io.github.gaming32.mckt.commands.CommandSource
import io.github.gaming32.mckt.commands.ConsoleCommandSource
import io.github.gaming32.mckt.commands.executesSuspend
import io.github.gaming32.mckt.config.ConfigErrorException
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

object ReloadCommand : BuiltinCommand {
    override val helpText = Component.text("Reload the server config")

    override fun buildTree() = literal<CommandSource>("reload")
        .requires { it.hasPermission(4) }
        .executesSuspend {
            try {
                source.server.reloadConfig(source !is ConsoleCommandSource)
            } catch (e: ConfigErrorException) {
                source.reply(Component.text("Failed to reload config:\n" + e.message, NamedTextColor.RED))
                return@executesSuspend 1
            }
            source.replyBroadcast(Component.text("Reloaded server config"))
            0
        }!!
}
