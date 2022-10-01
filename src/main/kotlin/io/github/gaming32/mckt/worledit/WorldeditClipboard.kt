package io.github.gaming32.mckt.worledit

import io.github.gaming32.mckt.BlockAccess
import io.github.gaming32.mckt.Blocks
import io.github.gaming32.mckt.data.writeVarInt
import io.github.gaming32.mckt.objects.BlockPosition
import io.github.gaming32.mckt.objects.BlockState
import io.github.gaming32.mckt.util.PalettedStorage

data class WorldeditClipboard(
    val size: BlockPosition,
    val pasteOffset: BlockPosition
) : BlockAccess {
    val data: PalettedStorage<BlockState> = PalettedStorage(
        size.x * size.y * size.z,
        Blocks.AIR
    ) { writeVarInt(it.globalId) }

    private fun blockIndex(x: Int, y: Int, z: Int) = y * size.x * size.z + z * size.x + x

    override fun getBlock(x: Int, y: Int, z: Int) = data[blockIndex(x, y, z)]

    override fun setBlock(x: Int, y: Int, z: Int, block: BlockState): Boolean {
        data[blockIndex(x, y, z)] = block
        return true
    }
}
