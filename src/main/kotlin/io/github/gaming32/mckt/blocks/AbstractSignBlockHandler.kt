package io.github.gaming32.mckt.blocks

import io.github.gaming32.mckt.blocks.entities.SignBlockEntity
import io.github.gaming32.mckt.objects.BlockPosition
import io.github.gaming32.mckt.objects.BlockState

abstract class AbstractSignBlockHandler : BlockHandler(), BlockEntityProvider<SignBlockEntity> {
    override fun createBlockEntity(pos: BlockPosition, state: BlockState) = SignBlockEntity(pos, state)
}
