package io.github.gaming32.mckt.items

import io.github.gaming32.mckt.*
import io.github.gaming32.mckt.nbt.NbtString
import io.github.gaming32.mckt.objects.*
import io.github.gaming32.mckt.packet.play.s2c.PlaySoundPacket
import io.github.gaming32.mckt.packet.play.s2c.SoundCategory

open class BlockItemHandler(val block: Identifier) : ItemHandler() {
    class ItemPlacementContext(
        client: PlayClient,
        hand: Hand,
        hit: BlockHitResult,
        world: World,
        itemStack: ItemStack
    ) : ItemUsageContext(client, hand, hit, world, itemStack) {
        constructor(ctx: ItemUsageContext) : this(ctx.client, ctx.hand, ctx.hit, ctx.world, ctx.itemStack)

        val placementPos = hit.location + hit.side.vector
        var replaceable = true
            private set

        override val location get() = if (replaceable) {
            super.location
        } else {
            placementPos
        }

        init {
            replaceable = world.getBlockImmediate(hit.location).canReplace(this)
        }

        fun canPlace() = replaceable || world.getBlockImmediate(location).canReplace(this)

        val placementDirections: Array<Direction> get() {
            val directions = Direction.getEntityFacingOrder(client)
            if (replaceable) return directions
            val side = this.side
            var i = 0
            while (i < directions.size && directions[i] != side.opposite) i++
            if (i > 0) {
                directions.copyInto(directions, 1, 0, i - 1)
                directions[0] = side.opposite
            }
            return directions
        }
    }

    override suspend fun useOnBlock(ctx: ItemUsageContext) =
        place(ItemPlacementContext(ctx))

    open suspend fun place(ctx: ItemPlacementContext): ActionResult {
        if (!ctx.canPlace()) {
            return ActionResult.FAIL
        }
        val useCtx = getPlacementContext(ctx) ?: return ActionResult.FAIL
        val state = getPlacementState(useCtx) ?: return ActionResult.FAIL
        if (!place(useCtx, state)) {
            return ActionResult.FAIL
        }
        val location = useCtx.location
        val world = useCtx.world
        val client = useCtx.client
        val stack = useCtx.itemStack
        var newState = world.getBlockImmediate(location)
        if (newState.blockId == state.blockId) {
            newState = placeFromTag(location, world, stack, newState)
            postPlacement(location, world, client, stack, newState)
            newState.getHandler(useCtx.server).onPlaced(world, location, newState, client, stack)
        }
        val soundGroup = BLOCK_PROPERTIES[newState.blockId]?.soundGroup
        if (soundGroup != null) {
            useCtx.server.broadcastExcept(client, PlaySoundPacket(
                soundGroup.placeSound,
                SoundCategory.BLOCK,
                Vector3d(location.x + 0.5, location.y + 0.5, location.z + 0.5),
                (soundGroup.volume + 1f) / 2f,
                (soundGroup.pitch * 0.8f)
            ))
        }
        if (!client.data.gamemode.defaultAbilities.creativeMode) {
            stack.decrement()
        }
        return ActionResult.success(false)
    }

    open suspend fun place(ctx: ItemPlacementContext, state: BlockState) =
        ctx.world.setBlock(ctx.location, state, SetBlockFlags.PERFORM_NEIGHBOR_UPDATE)

    open suspend fun getPlacementContext(ctx: ItemPlacementContext): ItemPlacementContext? = ctx

    open suspend fun getPlacementState(ctx: ItemPlacementContext) =
        ctx.server.getBlockHandler(block).getPlacementState(block, ctx)

    private suspend fun placeFromTag(
        location: BlockPosition,
        world: World,
        stack: ItemStack,
        state: BlockState
    ): BlockState {
        var newState = state
        val nbt = stack.extraNbt
        if (nbt != null) {
            val blockNbt = nbt.getCompound("BlockStateTag")
            blockNbt.forEach { (key, value) ->
                try {
                    newState = newState.with(key, value.cast<NbtString>().value)
                } catch (e: IllegalArgumentException) {
                    // Unknown property
                } catch (e: ClassCastException) {
                    // Value isn't a string
                }
            }
        }
        if (newState != state) {
            world.setBlock(location, newState, 0)
        }
        return newState
    }

    open suspend fun postPlacement(
        location: BlockPosition,
        world: World,
        client: PlayClient,
        stack: ItemStack,
        state: BlockState
    ) = Unit
}
