package io.github.gaming32.mckt.items

import io.github.gaming32.mckt.objects.BlockState

object LogItemHandler : ItemEventHandler {
    override suspend fun useOnBlock(event: ItemEventHandler.BlockUseEvent) =
        SimpleBlockItemHandler.setBlock(event, BlockState(
            blockId = event.item.itemId,
            properties = mapOf("axis" to event.face.axis.name.lowercase())
        ).canonicalize())
}
