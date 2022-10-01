package io.github.gaming32.mckt.commands.commands

import com.mojang.brigadier.arguments.LongArgumentType.longArg
import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import io.github.gaming32.mckt.BlockAccess
import io.github.gaming32.mckt.Blocks
import io.github.gaming32.mckt.GeneratorArgs
import io.github.gaming32.mckt.World
import io.github.gaming32.mckt.commands.CommandSource
import io.github.gaming32.mckt.commands.arguments.*
import io.github.gaming32.mckt.commands.executesSuspend
import io.github.gaming32.mckt.commands.wrap
import io.github.gaming32.mckt.dt.DtCompound
import io.github.gaming32.mckt.dt.DtInt
import io.github.gaming32.mckt.dt.DtString
import io.github.gaming32.mckt.items.WorldeditItem
import io.github.gaming32.mckt.objects.*
import io.github.gaming32.mckt.worledit.WorldeditClipboard
import io.github.gaming32.mckt.worledit.worldeditSession
import kotlinx.coroutines.yield
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

object WorldeditCommands {
    val NO_SELECTION_EXCEPTION = SimpleCommandExceptionType(Component.text("No selection!").wrap())
    val NO_CLIPBOARD_EXCEPTION = SimpleCommandExceptionType(Component.text("No clipboard!").wrap())

    object Pos1Command : BuiltinCommand {
        override val helpText = Component.text("Set position 1")

        override fun buildTree() = literal<CommandSource>("/pos1")
            .requires { it.hasPermission(1) }
            .executesSuspend {
                source.entity.worldeditSession.setPos1(source.entity.position.toBlockPosition())
                0
            }
            .then(argument<CommandSource, PositionArgument>("pos", BlockPositionArgumentType)
                .executesSuspend {
                    source.entity.worldeditSession.setPos1(getBlockPosition("pos"))
                    0
                }
            )!!
    }

    object Pos2Command : BuiltinCommand {
        override val helpText = Component.text("Set position 2")

        override fun buildTree() = literal<CommandSource>("/pos2")
            .requires { it.hasPermission(1) }
            .executesSuspend {
                source.entity.worldeditSession.setPos2(source.entity.position.toBlockPosition())
                0
            }
            .then(argument<CommandSource, PositionArgument>("pos", BlockPositionArgumentType)
                .executesSuspend {
                    source.entity.worldeditSession.setPos2(getBlockPosition("pos"))
                    0
                }
            )!!
    }

    object ChunkCommand : BuiltinCommand {
        override val helpText = Component.text("Set the selection to your current chunk.")

        override fun buildTree() = literal<CommandSource>("/chunk")
            .requires { it.hasPermission(1) }
            .executesSuspend { selectChunk(source.entity.position.toBlockPosition()) }
            .then(argument<CommandSource, PositionArgument>("location", BlockPositionArgumentType)
                .executesSuspend { selectChunk(getBlockPosition("location")) }
            )!!

        private suspend fun CommandContext<CommandSource>.selectChunk(pos: BlockPosition): Int {
            val cx = pos.x shr 4 shl 4
            val cz = pos.z shr 4 shl 4
            source.entity.worldeditSession.setPos1(BlockPosition(cx, -2032, cz))
            source.entity.worldeditSession.setPos2(BlockPosition(cx + 15, 2031, cz + 15))
            source.reply(Component.text("Selected the chunk ${cx shr 4}, ${cz shr 4}"))
            return 0
        }
    }

    object WandCommand : BuiltinCommand {
        override val helpText = Component.text("Get the wand item")

        override fun buildTree() = literal<CommandSource>("/wand")
            .requires { it.hasPermission(1) }
            .executesSuspend {
                source.entity.setInventorySlot(source.entity.data.selectedInventorySlot, createWand())
                0
            }!!
    }

    object SizeCommand : BuiltinCommand {
        override val helpText = Component.text("Get information about the selection")

        override fun buildTree() = literal<CommandSource>("/size")
            .requires { it.hasPermission(1) }
            .executesSuspend {
                val region = source.entity.worldeditSession.selection
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
            .requires { it.hasPermission(1) }
            .executesSuspend {
                source.entity.worldeditSession.clear()
                source.reply(Component.text("Selection cleared"))
                0
            }!!
    }

    object SetCommand : BuiltinCommand {
        override val helpText = Component.text("Sets all the blocks in the region")

        override fun buildTree() = literal<CommandSource>("/set")
            .requires { it.hasPermission(1) }
            .then(argument<CommandSource, BlockState>("block", BlockStateArgumentType)
                .executesSuspend {
                    val region = source.selection
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
        override val helpText = Component.text("Regenerates the contents of the selection")

        override fun buildTree() = literal<CommandSource>("/regen")
            .requires { it.hasPermission(1) }
            .executesSuspend {
                val world = source.server.world
                generate(world, world.worldGenerator)
            }
            .then(argument<CommandSource, Long>("seed", longArg())
                .executesSuspend {
                    val world = source.server.world
                    generate(world, world.meta.worldGenerator.createGenerator(getLong("seed")))
                }
            )!!

        private suspend fun CommandContext<CommandSource>.generate(
            world: World, generator: suspend (GeneratorArgs) -> Unit
        ): Int {
            val region = source.selection
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
                    generator(GeneratorArgs(object : BlockAccess {
                        override fun getBlock(location: BlockPosition) = chunk.getBlock(location)
                        override fun getBlock(x: Int, y: Int, z: Int) = chunk.getBlock(x, y, z)
                        override fun setBlock(location: BlockPosition, block: BlockState) =
                            if (location in chunkRegion) chunk.setBlock(location, block) else location.y in -2032..2031
                        override fun setBlock(x: Int, y: Int, z: Int, block: BlockState) =
                            if (chunkRegion.contains(x, y, z)) chunk.setBlock(x, y, z, block) else y in -2031..2031
                    }, world, chunkX, chunkZ))
                    yield()
                }
            }
            source.replyBroadcast(Component.text("Regenerated ${region.volume} blocks"))
            return region.volume
        }
    }

    object CopyCommand : BuiltinCommand {
        override val helpText = Component.text("Copy the selection to the clipboard")

        override fun buildTree() = literal<CommandSource>("/copy")
            .requires { it.hasPermission(1) }
            .executesSuspend {
                val region = source.selection
                val world = source.server.world
                val clipboard = WorldeditClipboard(region.size, region.min - source.entity.position.toBlockPosition())
                var i = 0
                (region - region.min).forEach { x, y, z ->
                    clipboard.setBlock(x, y, z, world.getLoadedBlock(
                        region.minX + x,
                        region.minY + y,
                        region.minZ + z
                    ))
                    if (i++ == 2 shl 16) {
                        i = 0
                        yield()
                    }
                }
                source.entity.worldeditSession.clipboard = clipboard
                source.reply(Component.text("Copied ${region.volume} blocks"))
                region.volume
            }!!
    }

    object CutCommand : BuiltinCommand {
        override val helpText = Component.text("Copy the selection to the clipboard")

        override fun buildTree() = literal<CommandSource>("/cut")
            .requires { it.hasPermission(1) }
            .executesSuspend { cut(Blocks.AIR) }
            .then(argument<CommandSource, BlockState>("replacement", BlockStateArgumentType)
                .executesSuspend { cut(getBlockState("replacement")) }
            )!!

        private suspend fun CommandContext<CommandSource>.cut(replacement: BlockState): Int {
            val region = source.selection
            val world = source.server.world
            val clipboard = WorldeditClipboard(region.size, region.min - source.entity.position.toBlockPosition())
            var i = 0
            (region - region.min).forEach { x, y, z ->
                clipboard.setBlock(x, y, z, world.getLoadedBlock(
                    region.minX + x,
                    region.minY + y,
                    region.minZ + z
                ))
                world.setBlock(
                    region.minX + x,
                    region.minY + y,
                    region.minZ + z,
                    replacement
                )
                if (i++ == 2 shl 16) {
                    i = 0
                    yield()
                }
            }
            source.entity.worldeditSession.clipboard = clipboard
            source.reply(Component.text("Cut ${region.volume} blocks"))
            return region.volume
        }
    }

    object PasteCommand : BuiltinCommand {
        override val helpText = Component.text("Paste the clipboard's contents")

        override fun buildTree() = literal<CommandSource>("/paste")
            .requires { it.hasPermission(1) }
            .executesSuspend {
                val clipboard = source.clipboard
                val origin = source.entity.position.toBlockPosition() + clipboard.pasteOffset
                val world = source.server.world
                var i = 0
                clipboard.size.toBlockBox().forEach { x, y, z ->
                    world.setBlock(origin.x + x, origin.y + y, origin.z + z, clipboard.getBlock(x, y, z))
                    if (i++ == 2 shl 16) {
                        i = 0
                        yield()
                    }
                }
                source.replyBroadcast(Component.text("Pasted ${clipboard.size.volume00} blocks"))
                clipboard.size.volume00
            }!!
    }

    object ClearClipboardCommand : BuiltinCommand {
        override val helpText = Component.text("Clear your clipboard")

        override fun buildTree() = literal<CommandSource>("clearclipboard")
            .requires { it.hasPermission(1) }
            .executesSuspend {
                source.entity.worldeditSession.clipboard = null
                source.reply(Component.text("Cleared your clipboard"))
                0
            }!!
    }

    private val CommandSource.selection get() =
        entity.worldeditSession.selection ?: throw NO_SELECTION_EXCEPTION.create()
    private val CommandSource.clipboard get() =
        entity.worldeditSession.clipboard ?: throw NO_CLIPBOARD_EXCEPTION.create()

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
