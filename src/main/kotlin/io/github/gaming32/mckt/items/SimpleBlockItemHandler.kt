package io.github.gaming32.mckt.items

import io.github.gaming32.mckt.BLOCK_PROPERTIES
import io.github.gaming32.mckt.BLOCK_SOUND_GROUPS
import io.github.gaming32.mckt.Blocks
import io.github.gaming32.mckt.GlobalPalette.DEFAULT_BLOCKSTATES
import io.github.gaming32.mckt.objects.ActionResult
import io.github.gaming32.mckt.objects.BlockState
import io.github.gaming32.mckt.objects.Vector3d
import io.github.gaming32.mckt.packet.play.s2c.PlaySoundPacket
import io.github.gaming32.mckt.packet.play.s2c.SoundCategory
import kotlinx.coroutines.CoroutineScope

object SimpleBlockItemHandler : ItemHandler() {
    override val isBlockItem get() = true

    override suspend fun useOnBlock(ctx: ItemUsageContext, scope: CoroutineScope) =
        setBlock(ctx, DEFAULT_BLOCKSTATES[ctx.item.itemId] ?: Blocks.STONE, true)

    suspend fun setBlock(
        ctx: ItemUsageContext,
        block: BlockState,
        playSound: Boolean = false
    ): ActionResult {
        val placePos = if (ctx.world.getBlock(ctx.location) == Blocks.AIR) {
            ctx.location
        } else {
            ctx.offsetLocation
        }
        ctx.server.setBlock(placePos, block)
        if (playSound) {
            BLOCK_PROPERTIES[block.blockId]?.let { properties ->
                BLOCK_SOUND_GROUPS[properties.soundGroup]?.let { soundGroup ->
                    ctx.server.broadcastExcept(ctx.client, PlaySoundPacket(
                        soundGroup.placeSound,
                        SoundCategory.BLOCK,
                        Vector3d(placePos.x + 0.5, placePos.y + 0.5, placePos.z + 0.5),
                        soundGroup.volume,
                        soundGroup.pitch
                    ))
                }
            }
        }
        return ActionResult.success(false)
    }
}
