package io.github.gaming32.mckt.blocks

import io.github.gaming32.mckt.GlobalPalette
import io.github.gaming32.mckt.items.BlockItemHandler
import io.github.gaming32.mckt.objects.Axis
import io.github.gaming32.mckt.objects.Axis.Companion.toAxis
import io.github.gaming32.mckt.objects.BlockRotation
import io.github.gaming32.mckt.objects.BlockState
import io.github.gaming32.mckt.objects.Identifier

object PillarBlockHandler : BlockHandler() {
    override suspend fun getPlacementState(
        block: Identifier,
        ctx: BlockItemHandler.ItemPlacementContext
    ) = GlobalPalette.DEFAULT_BLOCKSTATES[block]?.with("axis", ctx.side.axis.name.lowercase())

    override fun rotate(state: BlockState, rotation: BlockRotation) = when (rotation) {
        BlockRotation.COUNTERCLOCKWISE_90, BlockRotation.CLOCKWISE_90 -> when (state["axis"].toAxis()) {
            Axis.X -> state.with("axis", "z")
            Axis.Z -> state.with("axis", "x")
            else -> state
        }
        else -> state
    }
}
