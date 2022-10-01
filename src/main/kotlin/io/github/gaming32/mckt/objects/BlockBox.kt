package io.github.gaming32.mckt.objects

import kotlin.math.max
import kotlin.math.min

data class BlockBox(
    val minX: Int, val minY: Int, val minZ: Int, val maxX: Int, val maxY: Int, val maxZ: Int
) : Iterable<BlockPosition> {
    val sizeX get() = maxX - minX + 1
    val sizeY get() = maxY - minY + 1
    val sizeZ get() = maxZ - minZ + 1
    val size get() = BlockPosition(sizeX, sizeY, sizeZ)
    val volume get() = sizeX * sizeY * sizeZ

    constructor(a: BlockPosition, b: BlockPosition) : this(
        min(a.x, b.x), min(a.y, b.y), min(a.z, b.z),
        max(a.x, b.x), max(a.y, b.y), max(a.z, b.z)
    )

    inline fun forEach(action: (x: Int, y: Int, z: Int) -> Unit) {
        for (x in minX..maxX) {
            for (y in minY..maxY) {
                for (z in minZ..maxZ) {
                    action(x, y, z)
                }
            }
        }
    }

    override fun iterator() = iterator {
        forEach { x, y, z -> yield(BlockPosition(x, y, z)) }
    }

    operator fun contains(location: BlockPosition) =
        location.x in minX..maxX && location.y in minY..maxY && location.z in minZ..maxZ

    fun contains(x: Int, y: Int, z: Int) = x in minX..maxX && y in minY..maxY && z in minZ..maxZ
}
