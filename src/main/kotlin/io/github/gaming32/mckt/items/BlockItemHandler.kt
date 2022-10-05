package io.github.gaming32.mckt.items

import io.github.gaming32.mckt.*
import io.github.gaming32.mckt.dt.DtString
import io.github.gaming32.mckt.objects.*
import io.github.gaming32.mckt.packet.play.s2c.PlaySoundPacket
import io.github.gaming32.mckt.packet.play.s2c.SoundCategory
import kotlinx.coroutines.CoroutineScope

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
        val replaceable = world.getBlockImmediate(hit.location).canReplace(this)

        override val location = if (replaceable) {
            super.location
        } else {
            placementPos
        }

        fun canPlace() = replaceable || world.getBlockImmediate(location).canReplace(this)
    }

    override suspend fun useOnBlock(ctx: ItemUsageContext, scope: CoroutineScope) =
        place(ItemPlacementContext(ctx), scope)

    open suspend fun place(ctx: ItemPlacementContext, scope: CoroutineScope): ActionResult {
        if (!ctx.canPlace()) {
            return ActionResult.FAIL
        }
        val useCtx = getPlacementContext(ctx, scope) ?: return ActionResult.FAIL
        val state = getPlacementState(useCtx, scope) ?: return ActionResult.FAIL
        if (!place(useCtx, state, scope)) {
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

    open suspend fun place(ctx: ItemPlacementContext, state: BlockState, scope: CoroutineScope) =
        ctx.world.setBlock(ctx.location, state, SetBlockFlags.PERFORM_NEIGHBOR_UPDATE)

    open suspend fun getPlacementContext(ctx: ItemPlacementContext, scope: CoroutineScope): ItemPlacementContext? = ctx

    open suspend fun getPlacementState(ctx: ItemPlacementContext, scope: CoroutineScope) =
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
                    newState = newState.with(key, value.cast<DtString>().value)
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
