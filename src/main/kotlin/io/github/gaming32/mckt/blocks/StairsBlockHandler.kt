package io.github.gaming32.mckt.blocks

import io.github.gaming32.mckt.GlobalPalette.DEFAULT_BLOCKSTATES
import io.github.gaming32.mckt.MinecraftServer
import io.github.gaming32.mckt.PlayClient
import io.github.gaming32.mckt.items.BlockItemHandler
import io.github.gaming32.mckt.objects.*
import io.github.gaming32.mckt.world.World

class StairsBlockHandler(val id: Identifier, private val baseBlockState: BlockState) : BlockHandler() {
    private val defaultState = DEFAULT_BLOCKSTATES[id]!!

    override suspend fun onBroken(world: World, location: BlockPosition, state: BlockState) =
        world.server.getBlockHandler(id).onBroken(world, location, state)

    override suspend fun onUse(
        state: BlockState,
        world: World,
        pos: BlockPosition,
        client: PlayClient,
        hand: Hand,
        hit: BlockHitResult
    ) = baseBlockState.onUse(world, client, hand, hit)

    override suspend fun getPlacementState(block: Identifier, ctx: BlockItemHandler.ItemPlacementContext): BlockState {
        val side = ctx.side
        val pos = ctx.location
        val state = defaultState.with(mapOf(
            "facing" to ctx.playerFacing.name.lowercase(),
            "half" to if (side != Direction.DOWN && (side == Direction.UP || ctx.hitPos.y - pos.y <= 0.5)) {
                "bottom"
            } else {
                "top"
            }
        ))
        return state.with("shape", getStairShape(state, ctx.world, pos))
    }

    override suspend fun getStateForNeighborUpdate(
        state: BlockState,
        direction: Direction,
        neighborState: BlockState,
        world: World,
        pos: BlockPosition,
        neighborPos: BlockPosition
    ) = if (direction.axis.isHorizontal) {
        state.with("shape", getStairShape(state, world, pos))
    } else {
        state
    }

    private fun getStairShape(state: BlockState, world: World, pos: BlockPosition): String {
        val facing = Direction.valueOf(state["facing"].uppercase())

        var sideState = world.getBlockImmediate(pos + facing.vector)
        if (sideState.isStairs(world.server) && state["half"] == sideState["half"]) {
            val sideFacing = Direction.valueOf(sideState["facing"].uppercase())
            if (sideFacing.axis != facing.axis && isDifferentOrientation(state, world, pos, sideFacing.opposite)) {
                if (sideFacing == facing.rotateYCounterclockwise()) {
                    return "outer_left"
                }
                return "outer_right"
            }
        }

        sideState = world.getBlockImmediate(pos + facing.opposite.vector)
        if (sideState.isStairs(world.server) && state["half"] == sideState["half"]) {
            val sideFacing = Direction.valueOf(sideState["facing"].uppercase())
            if (sideFacing.axis != facing.axis && isDifferentOrientation(state, world, pos, sideFacing)) {
                if (sideFacing == facing.rotateYCounterclockwise()) {
                    return "inner_left"
                }
                return "inner_right"
            }
        }

        return "straight"
    }

    private fun isDifferentOrientation(
        state: BlockState,
        world: World,
        pos: BlockPosition,
        direction: Direction
    ): Boolean {
        val sideState = world.getBlockImmediate(pos + direction.vector)
        return !sideState.isStairs(world.server) ||
            sideState["facing"] != state["facing"] ||
            sideState["half"] != state["half"]
    }

    private fun BlockState.isStairs(server: MinecraftServer) = server.getBlockHandler(blockId) is StairsBlockHandler
}
