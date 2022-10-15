package io.github.gaming32.mckt.blocks

import io.github.gaming32.mckt.BLOCK_PROPERTIES
import io.github.gaming32.mckt.BlockMaterial
import io.github.gaming32.mckt.GlobalPalette.DEFAULT_BLOCKSTATES
import io.github.gaming32.mckt.PlayClient
import io.github.gaming32.mckt.items.BlockItemHandler
import io.github.gaming32.mckt.objects.*
import io.github.gaming32.mckt.packet.play.s2c.WorldEventPacket
import io.github.gaming32.mckt.world.Materials
import io.github.gaming32.mckt.world.SetBlockFlags
import io.github.gaming32.mckt.world.World

object TrapdoorBlockHandler : BlockHandler() {
    override suspend fun onUse(
        state: BlockState,
        world: World,
        pos: BlockPosition,
        client: PlayClient,
        hand: Hand,
        hit: BlockHitResult
    ): ActionResult {
        val material = BLOCK_PROPERTIES[state.blockId]!!.material
        if (material == Materials.METAL) return ActionResult.PASS
        val newState = state.cycle("open")
        world.setBlock(pos, newState, SetBlockFlags.DEFAULT_FLAGS)

        playToggleSound(client, world, pos, newState["open"].toBooleanStrict(), material)
        return ActionResult.success(false)
    }

    private suspend fun playToggleSound(
        skip: PlayClient,
        world: World,
        pos: BlockPosition,
        open: Boolean,
        material: BlockMaterial
    ) = world.server.broadcastExcept(skip, WorldEventPacket(
        if (open) {
            if (material == Materials.METAL) {
                WorldEventPacket.OPEN_METAL_TRAPDOOR
            } else {
                WorldEventPacket.OPEN_TRAPDOOR
            }
        } else {
            if (material == Materials.METAL) {
                WorldEventPacket.CLOSE_METAL_TRAPDOOR
            } else {
                WorldEventPacket.CLOSE_TRAPDOOR
            }
        },
        pos
    ))

    override suspend fun getPlacementState(block: Identifier, ctx: BlockItemHandler.ItemPlacementContext): BlockState {
        val state = DEFAULT_BLOCKSTATES[block]!!
        val side = ctx.side
        return if (!ctx.replaceable && side.axis.isHorizontal) {
            state.with(mapOf(
                "facing" to side.name.lowercase(),
                "half" to if (ctx.hitPos.y - ctx.location.y > 0.5) "top" else "bottom"
            ))
        } else {
            state.with(mapOf(
                "facing" to ctx.playerFacing.opposite.name.lowercase(),
                "half" to if (side == Direction.UP) "bottom" else "top"
            ))
        }
    }
}
