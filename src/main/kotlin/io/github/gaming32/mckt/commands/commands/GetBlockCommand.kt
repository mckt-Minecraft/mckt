package io.github.gaming32.mckt.commands.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import io.github.gaming32.mckt.commands.CommandSource
import io.github.gaming32.mckt.commands.arguments.BlockPositionArgumentType
import io.github.gaming32.mckt.commands.arguments.PositionArgument
import io.github.gaming32.mckt.commands.arguments.getBlockPosition
import io.github.gaming32.mckt.commands.arguments.getLoadedBlockPosition
import io.github.gaming32.mckt.commands.executesSuspend
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

object GetBlockCommand : BuiltinCommand {
    override val helpText = Component.text("Gets a block at a position")

    override fun buildTree() = literal<CommandSource>("getblock")
        .requires { it.hasPermission(1) }
        .then(argument<CommandSource, PositionArgument>("position", BlockPositionArgumentType)
            .executesSuspend {
                val position = getLoadedBlockPosition("position")
                val block = source.server.world.getBlock(position)!!
                source.reply(
                    Component.text("The block at ${position.x} ${position.y} ${position.z} is ")
                        .append(Component.text(block.toString(), NamedTextColor.GREEN))
                )
                0
            }
            .then(
                literal<CommandSource>("generate")
                .executesSuspend {
                    val position = getBlockPosition("position")
                    val block = source.server.world.getBlockOrGenerate(position)
                    source.reply(
                        Component.text("The block at ${position.x} ${position.y} ${position.z} is ")
                            .append(Component.text(block.toString(), NamedTextColor.GREEN))
                    )
                    0
                }
            )
        )!!
}
