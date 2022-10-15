package io.github.gaming32.mckt.blocks

import io.github.gaming32.mckt.GlobalPalette.DEFAULT_BLOCKSTATES
import io.github.gaming32.mckt.items.BlockItemHandler
import io.github.gaming32.mckt.objects.BlockPosition
import io.github.gaming32.mckt.objects.BlockState
import io.github.gaming32.mckt.objects.Direction
import io.github.gaming32.mckt.objects.Identifier
import io.github.gaming32.mckt.world.Blocks
import io.github.gaming32.mckt.world.World

object PaneBlockHandler : BlockHandler() {
    override suspend fun getPlacementState(
        block: Identifier,
        ctx: BlockItemHandler.ItemPlacementContext
    ): BlockState {
        val state = DEFAULT_BLOCKSTATES[block]!!
        val world = ctx.world
        val location = ctx.location
        val properties = mutableMapOf<String, String>()
        if (world.getBlock(location.north()) != Blocks.AIR) {
            properties["north"] = "true"
        }
        if (world.getBlock(location.south()) != Blocks.AIR) {
            properties["south"] = "true"
        }
        if (world.getBlock(location.west()) != Blocks.AIR) {
            properties["west"] = "true"
        }
        if (world.getBlock(location.east()) != Blocks.AIR) {
            properties["east"] = "true"
        }
        return state.with(properties)
    }

    override suspend fun getStateForNeighborUpdate(
        state: BlockState,
        direction: Direction,
        neighborState: BlockState,
        world: World,
        pos: BlockPosition,
        neighborPos: BlockPosition
    ): BlockState {
        if (!direction.axis.isHorizontal) return state
        return state.with(direction.name.lowercase(), (neighborState != Blocks.AIR).toString())
    }
}
