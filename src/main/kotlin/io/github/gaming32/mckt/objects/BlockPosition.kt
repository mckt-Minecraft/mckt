package io.github.gaming32.mckt.objects

import kotlinx.serialization.Serializable

@Serializable(BlockPositionSerializer::class)
data class BlockPosition(val x: Int, val y: Int, val z: Int) {
    companion object {
        val ZERO = BlockPosition(0, 0, 0)
        val UP = BlockPosition(0, 1, 0)
        val DOWN = BlockPosition(0, -1, 0)
        val NORTH = BlockPosition(0, 0, -1)
        val SOUTH = BlockPosition(0, 0, 1)
        val WEST = BlockPosition(-1, 0, 0)
        val EAST = BlockPosition(1, 0, 0)

        fun decodeFromLong(value: Long) = BlockPosition(
            (value shr 38).toInt(),
            (value shl 52 shr 52).toInt(),
            (value shl 26 shr 38).toInt()
        )
    }

    fun encodeToLong() =
        (x.toLong() and 0x3FFFFFFL shl 38) or (z.toLong() and 0x3FFFFFFL shl 12) or (y.toLong() and 0xFFFL)

    operator fun plus(other: BlockPosition) = BlockPosition(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: BlockPosition) = BlockPosition(x - other.x, y - other.y, z - other.z)
    operator fun times(other: Int) = BlockPosition(x * other, y * other, z * other)
    operator fun div(other: Int) = BlockPosition(x / other, y / other, z / other)

    fun up() = BlockPosition(x, y + 1, z)

    fun down() = BlockPosition(x, y - 1, z)

    fun toVector3d() = Vector3d(x + 0.5, y.toDouble(), z + 0.5)
}
