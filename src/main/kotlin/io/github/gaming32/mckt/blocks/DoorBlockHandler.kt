package io.github.gaming32.mckt.blocks

import io.github.gaming32.mckt.*
import io.github.gaming32.mckt.GlobalPalette.DEFAULT_BLOCKSTATES
import io.github.gaming32.mckt.items.BlockItemHandler
import io.github.gaming32.mckt.objects.*
import io.github.gaming32.mckt.packet.play.s2c.WorldEventPacket

object DoorBlockHandler : BlockHandler() {
    override suspend fun getStateForNeighborUpdate(
        state: BlockState,
        direction: Direction,
        neighborState: BlockState,
        world: World,
        pos: BlockPosition,
        neighborPos: BlockPosition
    ): BlockState {
        val half = state["half"]
        return if (direction.axis != Axis.Y || (half == "lower") != (direction == Direction.UP)) {
            if (half == "lower" && direction == Direction.DOWN && !state.canPlaceAt(world, pos)) {
                Blocks.AIR
            } else {
                state
            }
        } else {
            if (neighborState.blockId == state.blockId && neighborState["half"] != half) {
                state.with(mapOf(
                    "facing" to neighborState["facing"],
                    "open" to neighborState["open"],
                    "hinge" to neighborState["hinge"],
                    "powered" to neighborState["powered"]
                ))
            } else {
                Blocks.AIR
            }
        }
    }

    override suspend fun onBreak(
        world: World,
        pos: BlockPosition,
        state: BlockState,
        client: PlayClient
    ) {
        if (client.data.gamemode.defaultAbilities.creativeMode) {
            TallPlantBlockHandler.onBreakInCreative(world, pos, state, client)
        }
        super.onBreak(world, pos, state, client)
    }

    override suspend fun getPlacementState(
        block: Identifier,
        ctx: BlockItemHandler.ItemPlacementContext
    ): BlockState? {
        val pos = ctx.location
        val world = ctx.world
        return if (pos.y < 2031 && world.getBlock(pos.up()).canReplace(ctx)) {
            DEFAULT_BLOCKSTATES[block]?.with(mapOf(
                "facing" to ctx.playerFacing.name.lowercase(),
                "hinge" to getHinge(block, ctx),
                "half" to "lower"
            ))
        } else {
            null
        }
    }

    override suspend fun onPlaced(
        world: World,
        pos: BlockPosition,
        state: BlockState,
        client: PlayClient,
        stack: ItemStack
    ) {
        world.setBlock(pos.up(), state.with("half", "upper"), SetBlockFlags.PERFORM_NEIGHBOR_UPDATE)
    }

    private suspend fun getHinge(block: Identifier, ctx: BlockItemHandler.ItemPlacementContext): String {
        val blockView = ctx.world
        val blockPos = ctx.location
        val direction = ctx.playerFacing
        val blockPos2 = blockPos.up()
        val direction2 = direction.rotateYCounterclockwise()
        val blockPos3 = blockPos + direction2.vector
        val blockState = blockView.getBlock(blockPos3)
        val blockPos4 = blockPos2 + direction2.vector
        val blockState2 = blockView.getBlock(blockPos4)
        val direction3 = direction.rotateYClockwise()
        val blockPos5 = blockPos + direction3.vector
        val blockState3 = blockView.getBlock(blockPos5)
        val blockPos6 = blockPos2 + direction3.vector
        val blockState4 = blockView.getBlock(blockPos6)
        val i = ((if (blockState != Blocks.AIR) -1 else 0)
            + (if (blockState2 != Blocks.AIR) -1 else 0)
            + (if (blockState3 != Blocks.AIR) 1 else 0)
            + if (blockState4 != Blocks.AIR) 1 else 0)
        val bl = blockState.blockId == block && blockState["half"] == "lower"
        val bl2 = blockState3.blockId == block && blockState3["half"] == "lower"
        return if ((!bl || bl2) && i <= 0) {
            if ((!bl2 || bl) && i >= 0) {
                val j = direction.vector.x
                val k = direction.vector.z
                val vec3d = ctx.hit.position
                val d = vec3d.x - blockPos.x
                val e = vec3d.z - blockPos.z
                if ((j >= 0 || e >= 0.5) && (j <= 0 || e <= 0.5) && (k >= 0 || d <= 0.5) && (k <= 0 || d >= 0.5)) {
                    "left"
                } else {
                    "right"
                }
            } else {
                "left"
            }
        } else {
            "right"
        }
    }

    override suspend fun onUse(
        state: BlockState,
        world: World,
        pos: BlockPosition,
        client: PlayClient,
        hand: Hand,
        hit: BlockHitResult
    ) = if (BLOCK_PROPERTIES[state.blockId]?.material == Materials.METAL) {
        ActionResult.PASS
    } else {
        val newState = state.cycle("open")
        world.setBlock(pos, newState, 0)
        playSound(world, pos, state.blockId, newState["open"] == "true", client)
        ActionResult.success(false)
    }

    override suspend fun canPlaceAt(state: BlockState, world: World, pos: BlockPosition): Boolean {
        val down = pos.down()
        val downState = world.getBlock(down)
        return if (state["half"] == "lower") {
            downState != Blocks.AIR
        } else {
            downState.blockId == state.blockId
        }
    }

    private suspend fun playSound(
        world: World,
        pos: BlockPosition,
        block: Identifier,
        open: Boolean,
        exclude: PlayClient? = null
    ) {
        val isMetal = BLOCK_PROPERTIES[block]?.material == Materials.METAL
        world.server.broadcast(WorldEventPacket(
            if (open) {
                if (isMetal) {
                    WorldEventPacket.CLOSE_METAL_DOOR
                } else {
                    WorldEventPacket.CLOSE_DOOR
                }
            } else {
                if (isMetal) {
                    WorldEventPacket.OPEN_METAL_DOOR
                } else {
                    WorldEventPacket.OPEN_DOOR
                }
            }, pos
        )) { it !== exclude }
    }
}
