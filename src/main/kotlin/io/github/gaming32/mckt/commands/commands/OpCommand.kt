package io.github.gaming32.mckt.commands.commands

import com.mojang.brigadier.arguments.IntegerArgumentType.integer
import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import com.mojang.brigadier.context.CommandContext
import io.github.gaming32.mckt.commands.CommandSource
import io.github.gaming32.mckt.commands.EntitySelector
import io.github.gaming32.mckt.commands.arguments.getInteger
import io.github.gaming32.mckt.commands.arguments.getPlayers
import io.github.gaming32.mckt.commands.arguments.players
import io.github.gaming32.mckt.commands.executesSuspend
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import kotlin.math.min

object OpCommand : BuiltinCommand {
    override val helpText = Component.text("Sets a player's operator level")

    override fun buildTree() = literal<CommandSource>("op")
        .requires { it.hasPermission(3) }
        .then(argument<CommandSource, EntitySelector>("player", players())
            .executesSuspend { setLevel(min(2, source.operator)) }
            .then(argument<CommandSource, Int>("level", integer(0))
                .executesSuspend {
                    val level = getInteger("level")
                    if (level > source.operator) {
                        source.reply(Component.text(
                            "Cannot give player a higher operator level than you",
                            NamedTextColor.RED
                        ))
                        return@executesSuspend 1
                    }
                    setLevel(level)
                }
            )
        )!!

    private suspend fun CommandContext<CommandSource>.setLevel(level: Int): Int {
        getPlayers("player").forEach { player ->
            player.setOperatorLevel(level)
            source.replyBroadcast(Component.translatable(
                "commands.op.success",
                Component.text(player.username),
                Component.text(level)
            ))
        }
        return 0
    }
}
