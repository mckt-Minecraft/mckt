package io.github.gaming32.mckt.blocks

import io.github.gaming32.mckt.GlobalPalette
import io.github.gaming32.mckt.items.BlockItemHandler
import io.github.gaming32.mckt.objects.Identifier
import kotlinx.coroutines.CoroutineScope

object PillarBlockHandler : BlockHandler() {
    override suspend fun getPlacementState(
        block: Identifier,
        ctx: BlockItemHandler.ItemPlacementContext,
        scope: CoroutineScope
    ) = GlobalPalette.DEFAULT_BLOCKSTATES[block]?.with("axis", ctx.side.axis.name.lowercase())
}
