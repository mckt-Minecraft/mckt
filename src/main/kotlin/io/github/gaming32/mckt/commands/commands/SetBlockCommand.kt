package io.github.gaming32.mckt.commands.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import io.github.gaming32.mckt.commands.CommandSource
import io.github.gaming32.mckt.commands.arguments.*
import io.github.gaming32.mckt.commands.executesSuspend
import io.github.gaming32.mckt.objects.BlockState
import net.kyori.adventure.text.Component

object SetBlockCommand : BuiltinCommand {
    override val helpText = Component.text("Sets a block")

    override fun buildTree() = literal<CommandSource>("setblock")
        .requires { it.hasPermission(1) }
        .then(argument<CommandSource, PositionArgument>("pos", BlockPositionArgumentType)
            .then(argument<CommandSource, BlockState>("block", BlockStateArgumentType)
                .executesSuspend {
                    val location = getLoadedBlockPosition("pos")
                    source.server.world.setBlock(location, getBlockState("block"))
                    source.replyBroadcast(Component.translatable(
                        "commands.setblock.success",
                        Component.text(location.x),
                        Component.text(location.y),
                        Component.text(location.z)
                    ))
                    1
                }
            )
        )!!
}
