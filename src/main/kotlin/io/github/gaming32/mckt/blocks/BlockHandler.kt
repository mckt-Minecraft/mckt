package io.github.gaming32.mckt.blocks

import io.github.gaming32.mckt.PlayClient
import io.github.gaming32.mckt.World
import io.github.gaming32.mckt.objects.*
import io.github.gaming32.mckt.packet.play.s2c.WorldEventPacket
import kotlinx.coroutines.CoroutineScope

abstract class BlockHandler {
    open val requiresOperator get() = 0

    open suspend fun onUse(
        state: BlockState,
        world: World,
        location: BlockPosition,
        client: PlayClient,
        hand: Hand,
        hit: BlockHitResult,
        scope: CoroutineScope
    ) = ActionResult.PASS

    protected open suspend fun spawnBreakParticles(
        world: World,
        client: PlayClient,
        location: BlockPosition,
        state: BlockState
    ) = client.server.broadcastExcept(
        client,
        WorldEventPacket(WorldEventPacket.BREAK_BLOCK, location, state.globalId)
    )

    open suspend fun onBreak(
        world: World,
        location: BlockPosition,
        state: BlockState,
        client: PlayClient,
        scope: CoroutineScope
    ) = spawnBreakParticles(world, client, location, state)

    open suspend fun onBroken(
        world: World,
        location: BlockPosition,
        state: BlockState,
        scope: CoroutineScope
    ) = Unit

    open suspend fun afterBreak(
        world: World,
        client: PlayClient,
        location: BlockPosition,
        state: BlockState,
        stack: ItemStack,
        scope: CoroutineScope
    ) = Unit
}
