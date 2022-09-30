package io.github.gaming32.mckt.commands.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import io.github.gaming32.mckt.commands.CommandSource
import io.github.gaming32.mckt.commands.executesSuspend
import net.kyori.adventure.text.Component

object StopCommand : BuiltinCommand {
    override val helpText = Component.text("Stops the server")

    override fun buildTree() = literal<CommandSource>("stop")
        .requires { it.hasPermission(4) }
        .executesSuspend {
            source.replyBroadcast(Component.translatable("commands.stop.stopping"))
            source.server.running = false
            0
        }!!
}
