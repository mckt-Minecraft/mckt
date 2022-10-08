package io.github.gaming32.mckt.blocks

import io.github.gaming32.mckt.GlobalPalette.DEFAULT_BLOCKSTATES
import io.github.gaming32.mckt.items.BlockItemHandler
import io.github.gaming32.mckt.objects.BlockState
import io.github.gaming32.mckt.objects.Direction
import io.github.gaming32.mckt.objects.Identifier

object SlabBlockHandler : BlockHandler() {
    override suspend fun getPlacementState(block: Identifier, ctx: BlockItemHandler.ItemPlacementContext): BlockState? {
        val pos = ctx.location
        val state = ctx.world.getBlock(pos)
        if (state.blockId == block) {
            return state.with("type", "double")
        }
        val side = ctx.side
        val baseState = DEFAULT_BLOCKSTATES[block]!!
        return if (side != Direction.DOWN && (side == Direction.UP || ctx.hitPos.y - pos.y <= 0.5)) {
            baseState.with("type", "bottom")
        } else {
            baseState.with("type", "top")
        }
    }

    override fun canReplace(state: BlockState, ctx: BlockItemHandler.ItemPlacementContext): Boolean {
        val stack = ctx.itemStack
        val type = state["type"]
        if (type == "double" || stack.itemId != state.blockId) {
            return false
        }
        if (!ctx.replaceable) {
            return true
        }
        val isTop = ctx.hitPos.y - ctx.location.y > 0.5
        val side = ctx.side
        return if (type == "bottom") {
            side == Direction.UP || (isTop && side.axis.isHorizontal)
        } else {
            side == Direction.DOWN || (!isTop && side.axis.isHorizontal)
        }
    }
}
