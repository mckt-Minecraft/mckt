package io.github.gaming32.mckt.blocks

import io.github.gaming32.mckt.BLOCK_PROPERTIES
import io.github.gaming32.mckt.GlobalPalette
import io.github.gaming32.mckt.PlayClient
import io.github.gaming32.mckt.World
import io.github.gaming32.mckt.items.BlockItemHandler
import io.github.gaming32.mckt.items.ItemHandler
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

    open fun canReplace(state: BlockState, ctx: ItemHandler.ItemUsageContext) =
        BLOCK_PROPERTIES[state.blockId]?.material?.replaceable == true &&
            (ctx.itemStack.isEmpty() || ctx.itemStack.itemId != state.blockId)

    open suspend fun getPlacementState(
        block: Identifier,
        ctx: BlockItemHandler.ItemPlacementContext,
        scope: CoroutineScope
    ) = GlobalPalette.DEFAULT_BLOCKSTATES[ctx.itemStack.itemId]

    open fun onPlaced(
        world: World,
        location: BlockPosition,
        state: BlockState,
        client: PlayClient,
        stack: ItemStack
    ) = Unit

    open fun rotate(state: BlockState, rotation: BlockRotation) = state
}
