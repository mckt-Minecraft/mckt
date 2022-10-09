package io.github.gaming32.mckt.commands.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import io.github.gaming32.mckt.commands.CommandSource
import io.github.gaming32.mckt.commands.executesSuspend
import net.kyori.adventure.text.Component

object ReloadCommand : BuiltinCommand {
    override val helpText = Component.text("Reload the server config")

    override fun buildTree() = literal<CommandSource>("reload")
        .requires { it.hasPermission(4) }
        .executesSuspend {
            source.server.reloadConfig()
            source.reply(Component.text("Reloaded server config"))
            0
        }!!
}
