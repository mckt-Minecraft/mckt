package io.github.gaming32.mckt.commands.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import io.github.gaming32.mckt.Gamemode
import io.github.gaming32.mckt.commands.CommandSource
import io.github.gaming32.mckt.commands.EntitySelector
import io.github.gaming32.mckt.commands.arguments.getPlayers
import io.github.gaming32.mckt.commands.arguments.players
import io.github.gaming32.mckt.commands.executesSuspend
import net.kyori.adventure.text.Component

object GamemodeCommand : BuiltinCommand {
    override val helpText = Component.text("Set player gamemode")

    override fun buildTree() = literal<CommandSource>("gamemode").also { command ->
        command.requires { it.hasPermission(1) }
        Gamemode.values().forEach { gamemode ->
            val gamemodeText = Component.translatable("gameMode.${gamemode.name.lowercase()}")
            command.then(literal<CommandSource>(gamemode.name.lowercase())
                .executesSuspend {
                    source.player.setGamemode(gamemode)
                    source.replyBroadcast(Component.translatable("commands.gamemode.success.self", gamemodeText))
                    0
                }
                .then(argument<CommandSource, EntitySelector>("player", players())
                    .executesSuspend {
                        getPlayers("player").forEach { player ->
                            player.setGamemode(gamemode)
                            source.replyBroadcast(Component.translatable(
                                "commands.gamemode.success.other",
                                Component.text(player.username),
                                gamemodeText
                            ))
                        }
                        0
                    }
                )
            )
        }
    }!!
}
