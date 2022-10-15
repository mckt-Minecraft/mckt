package io.github.gaming32.mckt.blocks

import io.github.gaming32.mckt.GlobalPalette.DEFAULT_BLOCKSTATES
import io.github.gaming32.mckt.items.BlockItemHandler
import io.github.gaming32.mckt.objects.BlockPosition
import io.github.gaming32.mckt.objects.BlockState
import io.github.gaming32.mckt.objects.Direction
import io.github.gaming32.mckt.objects.Identifier
import io.github.gaming32.mckt.world.Blocks
import io.github.gaming32.mckt.world.World

object WallSignBlockHandler : AbstractSignBlockHandler() {
    override suspend fun canPlaceAt(state: BlockState, world: World, pos: BlockPosition) =
        world.getBlockImmediate(
            pos + Direction.valueOf(state["facing"].uppercase()).opposite.vector
        ).blockProperties.material.solid

    override suspend fun getPlacementState(block: Identifier, ctx: BlockItemHandler.ItemPlacementContext): BlockState? {
        var state = DEFAULT_BLOCKSTATES[block]!!
        val world = ctx.world
        val pos = ctx.location
        val directions = ctx.placementDirections
        for (direction in directions) {
            if (direction.axis.isHorizontal) {
                val opposite = direction.opposite
                state = state.with("facing", opposite.name.lowercase())
                if (state.canPlaceAt(world, pos)) {
                    return state
                }
            }
        }
        return null
    }

    override suspend fun getStateForNeighborUpdate(
        state: BlockState,
        direction: Direction,
        neighborState: BlockState,
        world: World,
        pos: BlockPosition,
        neighborPos: BlockPosition
    ) = if (direction.opposite == Direction.valueOf(state["facing"].uppercase()) && !state.canPlaceAt(world, pos)) {
        Blocks.AIR
    } else {
        state
    }
}