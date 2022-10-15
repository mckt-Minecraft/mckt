package io.github.gaming32.mckt.blocks

import io.github.gaming32.mckt.BLOCK_PROPERTIES
import io.github.gaming32.mckt.DEBUG
import io.github.gaming32.mckt.GlobalPalette
import io.github.gaming32.mckt.PlayClient
import io.github.gaming32.mckt.data.writeBlockPosition
import io.github.gaming32.mckt.data.writeVarLong
import io.github.gaming32.mckt.items.BlockItemHandler
import io.github.gaming32.mckt.objects.*
import io.github.gaming32.mckt.packet.play.PlayCustomPacket
import io.github.gaming32.mckt.packet.play.s2c.WorldEventPacket
import io.github.gaming32.mckt.world.World

abstract class BlockHandler {
    open val requiresOperator get() = 0

    open suspend fun onUse(
        state: BlockState,
        world: World,
        pos: BlockPosition,
        client: PlayClient,
        hand: Hand,
        hit: BlockHitResult
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
        pos: BlockPosition,
        state: BlockState,
        client: PlayClient
    ) = spawnBreakParticles(world, client, pos, state)

    open suspend fun onBroken(
        world: World,
        location: BlockPosition,
        state: BlockState
    ) = Unit

    open suspend fun afterBreak(
        world: World,
        client: PlayClient,
        location: BlockPosition,
        state: BlockState,
        stack: ItemStack
    ) = Unit

    open fun canReplace(state: BlockState, ctx: BlockItemHandler.ItemPlacementContext) =
        BLOCK_PROPERTIES[state.blockId]?.material?.replaceable == true &&
            (ctx.itemStack.isEmpty() || ctx.itemStack.itemId != state.blockId)

    open suspend fun getPlacementState(
        block: Identifier,
        ctx: BlockItemHandler.ItemPlacementContext
    ) = GlobalPalette.DEFAULT_BLOCKSTATES[ctx.itemStack.itemId]

    open suspend fun onPlaced(
        world: World,
        pos: BlockPosition,
        state: BlockState,
        client: PlayClient,
        stack: ItemStack
    ) = Unit

    open fun rotate(state: BlockState, rotation: BlockRotation) = state

    open suspend fun getStateForNeighborUpdate(
        state: BlockState,
        direction: Direction,
        neighborState: BlockState,
        world: World,
        pos: BlockPosition,
        neighborPos: BlockPosition
    ) = state

    open suspend fun canPlaceAt(state: BlockState, world: World, pos: BlockPosition) = true

    open suspend fun prepare(
        state: BlockState,
        world: World,
        pos: BlockPosition,
        flags: Int,
        maxUpdateDepth: Int = 512
    ) = Unit

    open suspend fun neighborUpdate(
        state: BlockState,
        world: World,
        pos: BlockPosition,
        block: Identifier,
        fromPos: BlockPosition,
        notify: Boolean
    ) {
        if (DEBUG) {
            world.server.broadcast(PlayCustomPacket(Identifier("debug/neighbors_update")) {
                writeVarLong(world.meta.time)
                writeBlockPosition(pos)
            })
        }
    }

    open fun onStateReplaced(
        state: BlockState,
        world: World,
        pos: BlockPosition,
        newState: BlockState,
        moved: Boolean
    ) {
        if (state.hasBlockEntity(world.server) && state.blockId != newState.blockId) {
            world.removeBlockEntity(pos)
        }
    }
}
