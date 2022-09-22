package io.github.gaming32.mckt.items

import io.github.gaming32.mckt.Blocks
import io.github.gaming32.mckt.objects.BlockState
import kotlinx.coroutines.CoroutineScope

object LogItemHandler : ItemHandler() {
    override val isBlockItem get() = true

    override suspend fun useOnBlock(ctx: ItemUsageContext, scope: CoroutineScope) =
        SimpleBlockItemHandler.setBlock(ctx, BlockState(
            blockId = ctx.item.itemId ?: Blocks.OAK_LOG.blockId,
            properties = mapOf("axis" to ctx.hit.side.axis.name.lowercase())
        ).canonicalize())
}
