package io.github.gaming32.mckt.items

import io.github.gaming32.mckt.Blocks
import io.github.gaming32.mckt.GlobalPalette.DEFAULT_BLOCKSTATES
import io.github.gaming32.mckt.objects.ActionResult
import io.github.gaming32.mckt.objects.BlockState
import kotlinx.coroutines.CoroutineScope

object SimpleBlockItemHandler : ItemHandler() {
    override val isBlockItem get() = true

    override suspend fun useOnBlock(ctx: ItemUsageContext, scope: CoroutineScope) =
        setBlock(ctx, DEFAULT_BLOCKSTATES[ctx.item.itemId] ?: Blocks.STONE)

    suspend fun setBlock(
        ctx: ItemUsageContext,
        block: BlockState
    ): ActionResult {
        val placePos = if (ctx.world.getBlock(ctx.hit.location) == Blocks.AIR) {
            ctx.hit.location
        } else {
            ctx.hit.offsetLocation
        }
        ctx.server.setBlock(placePos, block)
        return ActionResult.success(false)
    }
}
