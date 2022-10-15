package io.github.gaming32.mckt.world

import io.github.gaming32.mckt.objects.BlockPosition
import io.github.gaming32.mckt.objects.BlockState

interface SuspendingBlockAccess {
    suspend fun getBlock(x: Int, y: Int, z: Int): BlockState = getBlock(BlockPosition(x, y, z))

    suspend fun getBlock(pos: BlockPosition): BlockState = getBlock(pos.x, pos.y, pos.z)

    suspend fun setBlock(x: Int, y: Int, z: Int, block: BlockState): Boolean = setBlock(BlockPosition(x, y, z), block)

    suspend fun setBlock(pos: BlockPosition, block: BlockState): Boolean =
        setBlock(pos.x, pos.y, pos.z, block)
}

interface BlockAccess : SuspendingBlockAccess {
    override suspend fun getBlock(x: Int, y: Int, z: Int) = getBlockImmediate(x, y, z)
    override suspend fun getBlock(pos: BlockPosition) = getBlockImmediate(pos)
    override suspend fun setBlock(x: Int, y: Int, z: Int, block: BlockState) = setBlockImmediate(x, y, z, block)
    override suspend fun setBlock(pos: BlockPosition, block: BlockState) = setBlockImmediate(pos, block)

    fun getBlockImmediate(x: Int, y: Int, z: Int): BlockState = getBlockImmediate(BlockPosition(x, y, z))

    fun getBlockImmediate(pos: BlockPosition): BlockState =
        getBlockImmediate(pos.x, pos.y, pos.z)

    fun setBlockImmediate(x: Int, y: Int, z: Int, block: BlockState): Boolean =
        setBlockImmediate(BlockPosition(x, y, z), block)

    fun setBlockImmediate(pos: BlockPosition, block: BlockState): Boolean =
        setBlockImmediate(pos.x, pos.y, pos.z, block)
}

data class BlocksView(val inner: BlockAccess, val offsetX: Int, val offsetY: Int, val offsetZ: Int) : BlockAccess {
    constructor(inner: BlockAccess, offset: BlockPosition) : this(inner, offset.x, offset.y, offset.z)

    override suspend fun getBlock(x: Int, y: Int, z: Int) = inner.getBlock(x + offsetX, y + offsetY, z + offsetZ)

    override suspend fun setBlock(x: Int, y: Int, z: Int, block: BlockState) =
        inner.setBlock(x + offsetX, y + offsetY, z + offsetZ, block)

    override fun getBlockImmediate(x: Int, y: Int, z: Int) =
        inner.getBlockImmediate(x + offsetX, y + offsetY, z + offsetZ)

    override fun setBlockImmediate(x: Int, y: Int, z: Int, block: BlockState) =
        inner.setBlockImmediate(x + offsetX, y + offsetY, z + offsetZ, block)
}