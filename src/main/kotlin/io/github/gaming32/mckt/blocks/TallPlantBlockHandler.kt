package io.github.gaming32.mckt.blocks

import io.github.gaming32.mckt.PlayClient
import io.github.gaming32.mckt.objects.BlockPosition
import io.github.gaming32.mckt.objects.BlockState
import io.github.gaming32.mckt.packet.play.s2c.WorldEventPacket
import io.github.gaming32.mckt.world.Blocks
import io.github.gaming32.mckt.world.SetBlockFlags
import io.github.gaming32.mckt.world.World

object TallPlantBlockHandler {
    suspend fun onBreakInCreative(world: World, pos: BlockPosition, state: BlockState, client: PlayClient) {
        val half = state["half"]
        if (half != "upper") return
        val otherPos = pos.down()
        val otherState = world.getBlock(otherPos)
        if (otherState.blockId != state.blockId || otherState["half"] != "lower") return
        world.setBlock(otherPos, Blocks.AIR, SetBlockFlags.PERFORM_NEIGHBOR_UPDATE)
        world.server.broadcast(WorldEventPacket(WorldEventPacket.BREAK_BLOCK, otherPos, otherState.globalId))
    }
}
