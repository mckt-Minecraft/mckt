package io.github.gaming32.mckt.objects

import java.util.function.Consumer
import kotlin.math.max
import kotlin.math.min

data class BlockBox(
    val minX: Int, val minY: Int, val minZ: Int, val maxX: Int, val maxY: Int, val maxZ: Int
) : Iterable<BlockPosition> {
    val sizeX get() = maxX - minX
    val sizeY get() = maxY - minY
    val sizeZ get() = maxZ - minZ
    val size get() = sizeX * sizeY * sizeZ

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

    inline fun forEach(action: (BlockPosition) -> Unit) = forEach { x, y, z -> action(BlockPosition(x, y, z)) }

    @Suppress("UNCHECKED_CAST")
    override fun forEach(action: Consumer<in BlockPosition>) = forEach(action as (BlockPosition) -> Unit)

    fun asSequence() = sequence {
        forEach { yield(it) }
    }

    override fun iterator() = asSequence().iterator()
}
