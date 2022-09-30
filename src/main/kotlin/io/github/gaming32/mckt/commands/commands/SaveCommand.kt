package io.github.gaming32.mckt.commands.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import io.github.gaming32.mckt.commands.CommandSource
import io.github.gaming32.mckt.commands.executesSuspend
import net.kyori.adventure.text.Component

object SaveCommand : BuiltinCommand {
    override val helpText = Component.text("Saves the world")

    override fun buildTree() = literal<CommandSource>("save")
        .requires { it.hasPermission(4) }
        .executesSuspend {
            source.server.world.saveAndLog(source)
            0
        }!!
}
