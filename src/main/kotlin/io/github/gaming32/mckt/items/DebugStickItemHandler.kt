package io.github.gaming32.mckt.items

import io.github.gaming32.mckt.GlobalPalette
import io.github.gaming32.mckt.PlayClient
import io.github.gaming32.mckt.World
import io.github.gaming32.mckt.mod
import io.github.gaming32.mckt.objects.*
import kotlinx.coroutines.CoroutineScope
import net.kyori.adventure.text.Component

object DebugStickItemHandler : ItemHandler() {
    override suspend fun canMine(
        state: BlockState,
        world: World,
        location: BlockPosition,
        client: PlayClient,
        scope: CoroutineScope
    ): Boolean {
        use(client, state, world, location, false, client.data.getHeldItem(Hand.MAINHAND))
        return false
    }

    override suspend fun useOnBlock(ctx: ItemUsageContext, scope: CoroutineScope): ActionResult {
        val client = ctx.client
        val world = ctx.world
        val location = ctx.location
        if (!use(client, world.getBlockImmediate(location), world, location, true, ctx.itemStack)) {
            return ActionResult.FAIL
        }
        return ActionResult.success(false)
    }

    private suspend fun use(
        client: PlayClient,
        state: BlockState,
        world: World,
        location: BlockPosition,
        update: Boolean,
        item: ItemStack
    ): Boolean {
        if (client.data.operatorLevel < 1) {
            return false
        }
        val properties = GlobalPalette.BLOCK_STATE_PROPERTIES[state.blockId] ?: emptyMap()
        val blockIdString = state.blockId.toString()
        if (properties.isEmpty()) {
            client.sendMessage(Component.translatable(
                "item.minecraft.debug_stick.empty",
                Component.text(blockIdString)
            ), MessageType.ACTION_BAR)
            return false
        }
        val stickData = item.getOrCreateSubNbt("DebugProperty")
        var selectedName = stickData.getString(blockIdString)
        if (selectedName.isEmpty()) {
            selectedName = properties.keys.first()
        }
        if (update) {
            val newState = cycle(state, properties, selectedName, client.data.isSneaking)
            world.setBlock(location, newState)
            client.sendMessage(Component.translatable(
                "item.minecraft.debug_stick.update",
                Component.text(selectedName),
                Component.text(newState.properties[selectedName].orEmpty())
            ), MessageType.ACTION_BAR)
        } else {
            val newProperty = cycle(properties, selectedName, client.data.isSneaking)
            stickData.putString(blockIdString, newProperty)
            client.sendMessage(Component.translatable(
                "item.minecraft.debug_stick.select",
                Component.text(newProperty),
                Component.text(state.properties[newProperty].orEmpty())
            ), MessageType.ACTION_BAR)
        }
        return true
    }

    private fun cycle(
        state: BlockState,
        properties: Map<String, List<String>>,
        property: String,
        inverse: Boolean
    ): BlockState {
        val possibleValues = properties[property] ?: listOf()
        val currentIndex = possibleValues.indexOf(state.properties[property])
        return state.with(property, possibleValues[(currentIndex + if (inverse) -1 else 1) mod possibleValues.size])
    }

    private fun cycle(
        properties: Map<String, List<String>>,
        property: String,
        inverse: Boolean
    ): String {
        val possibleValues = properties.keys.toTypedArray()
        val currentIndex = possibleValues.indexOf(property)
        return possibleValues[(currentIndex + if (inverse) -1 else 1) mod possibleValues.size]
    }
}
