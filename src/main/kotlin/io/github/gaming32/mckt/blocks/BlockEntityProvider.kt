package io.github.gaming32.mckt.blocks

import io.github.gaming32.mckt.blocks.entities.BlockEntity
import io.github.gaming32.mckt.objects.BlockPosition
import io.github.gaming32.mckt.objects.BlockState

interface BlockEntityProvider<T : BlockEntity<T>> {
    fun createBlockEntity(pos: BlockPosition, state: BlockState): BlockEntity<T>
}
