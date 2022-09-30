package io.github.gaming32.mckt.items

import io.github.gaming32.mckt.PlayClient
import io.github.gaming32.mckt.World
import io.github.gaming32.mckt.objects.BlockPosition
import io.github.gaming32.mckt.objects.BlockState
import kotlinx.coroutines.CoroutineScope

object SwordItemHandler : ItemHandler() {
    override suspend fun canMine(
        state: BlockState,
        world: World,
        location: BlockPosition,
        client: PlayClient,
        scope: CoroutineScope
    ): Boolean {
        if (client.data.gamemode.defaultAbilities.creativeMode) {
            return false
        }
        return super.canMine(state, world, location, client, scope)
    }
}
