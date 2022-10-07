package io.github.gaming32.mckt.items

import io.github.gaming32.mckt.World
import io.github.gaming32.mckt.blocks.Fertilizable
import io.github.gaming32.mckt.objects.ActionResult
import io.github.gaming32.mckt.objects.BlockPosition
import io.github.gaming32.mckt.objects.ItemStack
import io.github.gaming32.mckt.packet.play.s2c.WorldEventPacket
import kotlin.random.Random

object BoneMealItemHandler : ItemHandler() {
    override suspend fun useOnBlock(ctx: ItemUsageContext): ActionResult {
        val world = ctx.world
        val location = ctx.location
        if (useOnFertilizable(ctx.itemStack, world, location)) {
            ctx.server.broadcast(WorldEventPacket(WorldEventPacket.USE_BONEMEAL, location))
            return ActionResult.success(false)
        }
        return ActionResult.PASS
    }

    suspend fun useOnFertilizable(stack: ItemStack, world: World, location: BlockPosition): Boolean {
        val block = world.getBlockImmediate(location)
        val handler = block.getHandler(world.server)
        if (handler is Fertilizable && handler.isFertilizable(world, location, block)) {
            if (handler.canGrow(world, Random, location, block)) {
                handler.grow(world, Random, location, block)
            }
            stack.decrement()
            return true
        }
        return false
    }
}
