package io.github.gaming32.mckt.commands.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import io.github.gaming32.mckt.BlockAccess
import io.github.gaming32.mckt.Blocks
import io.github.gaming32.mckt.GeneratorArgs
import io.github.gaming32.mckt.commands.CommandSource
import io.github.gaming32.mckt.commands.arguments.*
import io.github.gaming32.mckt.commands.executesSuspend
import io.github.gaming32.mckt.commands.wrap
import io.github.gaming32.mckt.dt.DtCompound
import io.github.gaming32.mckt.dt.DtInt
import io.github.gaming32.mckt.dt.DtString
import io.github.gaming32.mckt.items.WorldeditItem
import io.github.gaming32.mckt.objects.*
import io.github.gaming32.mckt.worledit.worldeditSelection
import kotlinx.coroutines.yield
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

object WorldeditCommands {
    val NO_SELECTION_EXCEPTION = SimpleCommandExceptionType(Component.text("No selection!").wrap())

    object Pos1Command : BuiltinCommand {
        override val helpText = Component.text("Set position 1")

        override fun buildTree() = literal<CommandSource>("/pos1")
            .executesSuspend {
                source.entity.worldeditSelection.setPos1(source.entity.position.toBlockPosition())
                0
            }
            .then(argument<CommandSource, PositionArgument>("pos", BlockPositionArgumentType)
                .executesSuspend {
                    source.entity.worldeditSelection.setPos1(getBlockPosition("pos"))
                    0
                }
            )!!
    }

    object Pos2Command : BuiltinCommand {
        override val helpText = Component.text("Set position 2")

        override fun buildTree() = literal<CommandSource>("/pos2")
            .executesSuspend {
                source.entity.worldeditSelection.setPos2(source.entity.position.toBlockPosition())
                0
            }
            .then(argument<CommandSource, PositionArgument>("pos", BlockPositionArgumentType)
                .executesSuspend {
                    source.entity.worldeditSelection.setPos2(getBlockPosition("pos"))
                    0
                }
            )!!
    }

    object WandCommand : BuiltinCommand {
        override val helpText = Component.text("Get the wand item")

        override fun buildTree() = literal<CommandSource>("/wand")
            .executesSuspend {
                source.entity.setInventorySlot(source.entity.data.selectedInventorySlot, createWand())
                0
            }!!
    }

    object SizeCommand : BuiltinCommand {
        override val helpText = Component.text("Get information about the selection")

        override fun buildTree() = literal<CommandSource>("/size")
            .executesSuspend {
                val region = source.entity.worldeditSelection.toBlockBox()
                if (region == null) {
                    source.reply(Component.text("No region selected", NamedTextColor.RED))
                    return@executesSuspend 1
                }
                source.reply(Component.text("The region's size is ${region.size}"))
                source.reply(Component.text("The region's diagonal distance is ${region.size.toVector3d().magnitude}"))
                source.reply(Component.text("The region's volume is ${region.volume}"))
                0
            }!!
    }

    object SelCommand : BuiltinCommand {
        override val helpText = Component.text("Clear selection")

        override val aliases = listOf(";", "/desel", "/deselect")

        override fun buildTree() = literal<CommandSource>("/sel")
            .executesSuspend {
                source.entity.worldeditSelection.clear()
                source.reply(Component.text("Selection cleared"))
                0
            }!!
    }

    object SetCommand : BuiltinCommand {
        override val helpText = Component.text("Sets all the blocks in the region")

        override fun buildTree() = literal<CommandSource>("/set")
            .then(argument<CommandSource, BlockState>("block", BlockStateArgumentType)
                .executesSuspend {
                    val region = source.entity.worldeditSelection.toBlockBox() ?: throw NO_SELECTION_EXCEPTION.create()
                    val block = getBlockState("block")
                    val world = source.server.world
                    var i = 0
                    region.forEach { x, y, z ->
                        world.setBlock(x, y, z, block)
                        if (i++ == 2 shl 16) {
                            i = 0
                            yield()
                        }
                    }
                    source.replyBroadcast(Component.translatable(
                        "commands.fill.success",
                        Component.text(region.volume)
                    ))
                    region.volume
                }
            )!!
    }

    object RegenCommand : BuiltinCommand {
        override val helpText = Component.text("Put the region back to its earliest seed-generated form.")

        override fun buildTree() = literal<CommandSource>("/regen")
            .executesSuspend {
                val region = source.entity.worldeditSelection.toBlockBox() ?: throw NO_SELECTION_EXCEPTION.create()
                val world = source.server.world
                for (chunkX in (region.minX shr 4)..(region.maxX shr 4)) {
                    for (chunkZ in (region.minZ shr 4)..(region.maxZ shr 4)) {
                        val chunk = world.getChunk(chunkX, chunkZ)
                            ?: continue // The chunk was never loaded, so therefore it will be generated on first load.
                        val chunkRegion = BlockBox(
                            region.minX - (chunkX shl 4),
                            region.minY,
                            region.minZ - (chunkZ shl 4),
                            region.maxX - (chunkX shl 4),
                            region.maxY,
                            region.maxZ - (chunkZ shl 4)
                        )
                        chunkRegion.forEach { x, y, z ->
                            if (x in 0..15 && z in 0..15) {
                                chunk.setBlock(x, y, z, Blocks.AIR)
                            }
                        }
                        world.worldGenerator(GeneratorArgs(object : BlockAccess {
                            override fun getBlock(location: BlockPosition) = chunk.getBlock(location)
                            override fun getBlock(x: Int, y: Int, z: Int) = chunk.getBlock(x, y, z)
                            override fun setBlock(location: BlockPosition, block: BlockState) =
                                if (location in chunkRegion) chunk.setBlock(location, block) else Unit
                            override fun setBlock(x: Int, y: Int, z: Int, block: BlockState) =
                                if (chunkRegion.contains(x, y, z)) chunk.setBlock(x, y, z, block) else Unit
                        }, world, chunkX, chunkZ))
                        yield()
                    }
                }
                source.replyBroadcast(Component.text("Regenerated ${region.volume} blocks"))
                region.volume
            }!!
    }

    fun createWand() = ItemStack(
        Identifier("wooden_axe"),
        extraNbtInternal = DtCompound(
            "display" to DtCompound(
                "Name" to DtString("{\"text\":\"WorldEdit Wand\",\"color\":\"light_purple\",\"italic\":\"false\"}")
            ),
            "Worldedit" to DtCompound(
                "Type" to DtInt(WorldeditItem.TYPE_WAND)
            )
        )
    )
}
