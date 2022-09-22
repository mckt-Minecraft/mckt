package io.github.gaming32.mckt.items

import io.github.gaming32.mckt.PlayClient
import io.github.gaming32.mckt.World
import io.github.gaming32.mckt.objects.*
import kotlinx.coroutines.CoroutineScope

abstract class ItemHandler {
    open class ItemUsageContext protected constructor(
        val client: PlayClient,
        val hand: Hand,
        val hit: BlockHitResult,
        val world: World,
        val itemStack: ItemStack
    ) {
        constructor(client: PlayClient, hand: Hand, hit: BlockHitResult) : this(
            client, hand, hit, client.server.world, client.data.getHeldItem(hand)
        )

        val item get() = client.data.getHeldItem(hand)

        val server get() = client.server
    }

    open val isBlockItem get() = false

    open suspend fun canMine(
        state: BlockState,
        world: World,
        position: BlockPosition,
        client: PlayClient,
        scope: CoroutineScope
    ) = true

    open suspend fun useOnBlock(ctx: ItemUsageContext, scope: CoroutineScope) = ActionResult.PASS

    open suspend fun use(world: World, client: PlayClient, hand: Hand, scope: CoroutineScope) =
        client.data.getHeldItem(hand).pass()

    open suspend fun postMine(
        item: ItemStack,
        world: World,
        state: BlockState,
        location: BlockPosition,
        client: PlayClient,
        scope: CoroutineScope
    ) = false
}
