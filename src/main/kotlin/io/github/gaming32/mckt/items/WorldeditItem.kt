package io.github.gaming32.mckt.items

import io.github.gaming32.mckt.PlayClient
import io.github.gaming32.mckt.World
import io.github.gaming32.mckt.castOrNull
import io.github.gaming32.mckt.nbt.NbtInt
import io.github.gaming32.mckt.objects.*
import io.github.gaming32.mckt.worledit.worldeditSession

object WorldeditItem : ItemHandler() {
    const val TYPE_NONE = -1
    const val TYPE_WAND = 0

    override suspend fun canMine(
        state: BlockState,
        world: World,
        location: BlockPosition,
        client: PlayClient
    ): Boolean {
        val item = client.data.getHeldItem(Hand.MAINHAND)
        val type = item.worldeditType
        if (type == TYPE_NONE) {
            return super.canMine(state, world, location, client)
        }
        when (type) {
            TYPE_WAND -> client.worldeditSession.setPos1(location)
        }
        return false
    }

    override suspend fun useOnBlock(ctx: ItemUsageContext): ActionResult {
        val type = ctx.itemStack.worldeditType
        if (type == TYPE_NONE) {
            return super.useOnBlock(ctx)
        }
        when (type) {
            TYPE_WAND -> ctx.client.worldeditSession.setPos2(ctx.location)
        }
        return ActionResult.success(true)
    }

    var ItemStack.worldeditType: Int
        get() = this["Worldedit"]?.get("Type").castOrNull<NbtInt>()?.value ?: TYPE_NONE
        set(type) {
            getOrCreateSubNbt("Worldedit")["Type"] = NbtInt(type)
        }
}
