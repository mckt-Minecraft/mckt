package io.github.gaming32.mckt.commands.commands

import com.mojang.brigadier.arguments.IntegerArgumentType.integer
import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import io.github.gaming32.mckt.commands.CommandSource
import io.github.gaming32.mckt.commands.arguments.getInt
import io.github.gaming32.mckt.commands.executesSuspend
import net.kyori.adventure.text.Component

object TpsCommand : BuiltinCommand {
    override val helpText = Component.text("Set the server TPS")

    override fun buildTree() = literal<CommandSource>("tps")
        .executesSuspend {
            source.reply(Component.text(
                "The server's target TPS is ${source.server.targetTps}. " +
                    "Ticks should take ${source.server.targetMspt}ms."
            ))
            0
        }
        .then(argument<CommandSource, Int>("tps", integer())
            .requires { it.hasPermission(4) }
            .executesSuspend {
                val target = getInt("tps")
                source.server.targetTps = target
                source.replyBroadcast(Component.text(
                    "The server's target TPS is now $target. Ticks should take ${source.server.targetMspt}ms."
                ))
                0
            }
        )!!
}
