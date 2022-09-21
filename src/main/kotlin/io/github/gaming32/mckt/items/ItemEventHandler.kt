package io.github.gaming32.mckt.items

import io.github.gaming32.mckt.PlayClient
import io.github.gaming32.mckt.objects.BlockPosition
import io.github.gaming32.mckt.objects.BlockState
import io.github.gaming32.mckt.objects.Direction
import io.github.gaming32.mckt.objects.ItemStack
import io.github.gaming32.mckt.packet.play.s2c.SetBlockPacket

interface ItemEventHandler {
    open class UseEvent(
        val item: ItemStack,
        val client: PlayClient,
        val offhand: Boolean,
        val sequence: Int
    ) {
        val server = client.server
        val world = server.world

        suspend fun setBlock(location: BlockPosition, block: BlockState) {
            world.setBlock(location, block)
            server.broadcast(SetBlockPacket(location, block))
        }
    }

    class BlockUseEvent(
        item: ItemStack,
        client: PlayClient,
        offhand: Boolean,
        sequence: Int,
        val location: BlockPosition,
        val face: Direction,
        val cursorX: Float = 0f,
        val cursorY: Float = 0f,
        val cursorZ: Float = 0f,
        val insideBlock: Boolean = false
    ) : UseEvent(item, client, offhand, sequence)

    enum class Result {
        SUCCESS, USE_UP, FAILURE
    }

    suspend fun use(event: UseEvent) = Result.SUCCESS

    suspend fun useOnBlock(event: BlockUseEvent) = use(event)
}
