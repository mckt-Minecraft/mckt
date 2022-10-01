package io.github.gaming32.mckt.commands.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import io.github.gaming32.mckt.coerceToInt
import io.github.gaming32.mckt.commands.CommandSource
import io.github.gaming32.mckt.commands.arguments.*
import io.github.gaming32.mckt.commands.executesSuspend
import io.github.gaming32.mckt.objects.BlockBox
import io.github.gaming32.mckt.objects.BlockState
import net.kyori.adventure.text.Component

object FillCommand : BuiltinCommand {
    override val helpText = Component.text("Fills an area")

    override fun buildTree() = literal<CommandSource>("fill")
        .requires { it.hasPermission(1) }
        .then(argument<CommandSource, PositionArgument>("from", BlockPositionArgumentType)
            .then(argument<CommandSource, PositionArgument>("to", BlockPositionArgumentType)
                .then(argument<CommandSource, BlockState>("block", BlockStateArgumentType)
                    .executesSuspend {
                        val box = BlockBox(getLoadedBlockPosition("from"), getLoadedBlockPosition("to"))
                        val block = getBlockState("block")
                        val world = source.server.world
                        box.forEach { x, y, z ->
                            world.setBlock(x, y, z, block)
                        }
                        source.reply(Component.translatable(
                            "commands.fill.success",
                            Component.text(box.volume.toString())
                        ))
                        box.volume.coerceToInt()
                    }
                )
            )
        )!!
}
