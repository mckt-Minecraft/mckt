package io.github.gaming32.mckt.blocks

import io.github.gaming32.mckt.GlobalPalette.DEFAULT_BLOCKSTATES
import io.github.gaming32.mckt.items.BlockItemHandler
import io.github.gaming32.mckt.objects.BlockPosition
import io.github.gaming32.mckt.objects.BlockState
import io.github.gaming32.mckt.objects.Direction
import io.github.gaming32.mckt.objects.Identifier
import io.github.gaming32.mckt.world.Blocks
import io.github.gaming32.mckt.world.World
import kotlin.math.floor

object SignBlockHandler : AbstractSignBlockHandler() {
    override suspend fun canPlaceAt(state: BlockState, world: World, pos: BlockPosition) =
        world.getBlockImmediate(pos.down()).blockProperties.material.solid

    override suspend fun getPlacementState(block: Identifier, ctx: BlockItemHandler.ItemPlacementContext) =
        DEFAULT_BLOCKSTATES[block]?.with(
            "rotation", (floor(ctx.client.data.yaw / 22.5f + 8.5f).toInt() and 15).toString()
        )

    override suspend fun getStateForNeighborUpdate(
        state: BlockState,
        direction: Direction,
        neighborState: BlockState,
        world: World,
        pos: BlockPosition,
        neighborPos: BlockPosition
    ) = if (direction == Direction.DOWN && !canPlaceAt(state, world, pos)) {
        Blocks.AIR
    } else {
        state
    }
}
