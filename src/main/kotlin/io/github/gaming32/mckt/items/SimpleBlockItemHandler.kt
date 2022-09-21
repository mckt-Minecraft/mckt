package io.github.gaming32.mckt.items

import io.github.gaming32.mckt.Blocks
import io.github.gaming32.mckt.DEFAULT_BLOCKSTATES
import io.github.gaming32.mckt.objects.BlockState

object SimpleBlockItemHandler : ItemEventHandler {
    override suspend fun useOnBlock(event: ItemEventHandler.BlockUseEvent) =
        setBlock(event, DEFAULT_BLOCKSTATES[event.item.itemId] ?: Blocks.STONE)

    suspend fun setBlock(
        event: ItemEventHandler.BlockUseEvent,
        block: BlockState
    ): ItemEventHandler.Result {
        val placePos = if (event.world.getBlock(event.location) == Blocks.AIR) {
            event.location
        } else {
            event.location + event.face.vector
        }
        event.server.setBlock(placePos, block)
        return ItemEventHandler.Result.USE_UP
    }
}
