package io.github.gaming32.mckt.items

import io.github.gaming32.mckt.PlayClient
import io.github.gaming32.mckt.blocks.entities.SignBlockEntity
import io.github.gaming32.mckt.objects.BlockPosition
import io.github.gaming32.mckt.objects.BlockState
import io.github.gaming32.mckt.objects.Identifier
import io.github.gaming32.mckt.objects.ItemStack
import io.github.gaming32.mckt.packet.play.s2c.OpenSignEditorPacket
import io.github.gaming32.mckt.packet.play.s2c.SetBlockPacket
import io.github.gaming32.mckt.world.World

class SignItemHandler(block: Identifier, wallBlock: Identifier) : WallStandingBlockItemHandler(block, wallBlock) {
    override suspend fun postPlacement(
        location: BlockPosition,
        world: World,
        client: PlayClient,
        stack: ItemStack,
        state: BlockState
    ) {
        val entity = world.getBlockEntity(location) as SignBlockEntity
        entity.editor = client.uuid
        client.sendPacket(SetBlockPacket(world, entity.pos))
        client.sendPacket(OpenSignEditorPacket(entity.pos))
    }
}
