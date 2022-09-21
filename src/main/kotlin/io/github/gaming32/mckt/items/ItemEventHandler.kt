package io.github.gaming32.mckt.items

import io.github.gaming32.mckt.PlayClient
import io.github.gaming32.mckt.objects.BlockPosition
import io.github.gaming32.mckt.objects.Direction
import io.github.gaming32.mckt.objects.ItemStack
import kotlinx.coroutines.CoroutineScope

interface ItemEventHandler {
    open class UseEvent(
        val item: ItemStack,
        val client: PlayClient,
        val scope: CoroutineScope,
        val offhand: Boolean,
        val sequence: Int
    ) {
        val server = client.server
        val world = server.world
    }

    class BlockUseEvent(
        item: ItemStack,
        client: PlayClient,
        scope: CoroutineScope,
        offhand: Boolean,
        sequence: Int,
        val location: BlockPosition,
        val face: Direction,
        val cursorX: Float = 0f,
        val cursorY: Float = 0f,
        val cursorZ: Float = 0f,
        val insideBlock: Boolean = false
    ) : UseEvent(item, client, scope, offhand, sequence)

    enum class Result {
        SUCCESS, USE_UP, PASS
    }

    suspend fun use(event: UseEvent) = Result.PASS

    suspend fun useOnBlock(event: BlockUseEvent) = use(event)
}
