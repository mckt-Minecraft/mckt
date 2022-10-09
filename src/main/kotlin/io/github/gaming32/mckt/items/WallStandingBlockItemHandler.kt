package io.github.gaming32.mckt.items

import io.github.gaming32.mckt.objects.BlockState
import io.github.gaming32.mckt.objects.Direction
import io.github.gaming32.mckt.objects.Identifier

open class WallStandingBlockItemHandler(block: Identifier, val wallBlock: Identifier) : BlockItemHandler(block) {
    override suspend fun getPlacementState(ctx: ItemPlacementContext): BlockState? {
        val wallState = ctx.server.getBlockHandler(wallBlock).getPlacementState(wallBlock, ctx)
        val world = ctx.world
        val pos = ctx.location
        for (direction in ctx.placementDirections) {
            if (direction == Direction.UP) continue
            val checkState = if (direction == Direction.DOWN) {
                ctx.server.getBlockHandler(block).getPlacementState(block, ctx)
            } else {
                wallState
            }
            if (checkState?.canPlaceAt(world, pos) == true) {
                return checkState
            }
        }
        return null
    }
}
