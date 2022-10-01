package io.github.gaming32.mckt.items

import io.github.gaming32.mckt.PlayClient
import io.github.gaming32.mckt.World
import io.github.gaming32.mckt.castOrNull
import io.github.gaming32.mckt.dt.DtInt
import io.github.gaming32.mckt.objects.*
import io.github.gaming32.mckt.worledit.worldeditSelection
import kotlinx.coroutines.CoroutineScope

object WorldeditItem : ItemHandler() {
    const val TYPE_NONE = -1
    const val TYPE_WAND = 0

    override suspend fun canMine(
        state: BlockState,
        world: World,
        location: BlockPosition,
        client: PlayClient,
        scope: CoroutineScope
    ): Boolean {
        val item = client.data.getHeldItem(Hand.MAINHAND)
        val type = item.worldeditType
        if (type == TYPE_NONE) {
            return super.canMine(state, world, location, client, scope)
        }
        when (type) {
            TYPE_WAND -> client.worldeditSelection.setPos1(location)
        }
        return false
    }

    override suspend fun useOnBlock(ctx: ItemUsageContext, scope: CoroutineScope): ActionResult {
        val type = ctx.itemStack.worldeditType
        if (type == TYPE_NONE) {
            return super.useOnBlock(ctx, scope)
        }
        when (type) {
            TYPE_WAND -> ctx.client.worldeditSelection.setPos2(ctx.location)
        }
        return ActionResult.success(true)
    }

    var ItemStack.worldeditType: Int
        get() = this["Worldedit"]?.get("Type").castOrNull<DtInt>()?.value ?: TYPE_NONE
        set(type) {
            getOrCreateSubNbt("Worldedit")["Type"] = DtInt(type)
        }
}