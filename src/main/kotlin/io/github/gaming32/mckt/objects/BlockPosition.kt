package io.github.gaming32.mckt.objects

import kotlinx.serialization.Serializable

@Serializable(BlockPositionSerializer::class)
data class BlockPosition(val x: Int, val y: Int, val z: Int) {
    companion object {
        val ZERO = BlockPosition(0, 0, 0)

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

    fun up() = BlockPosition(x, y + 1, z)

    fun down() = BlockPosition(x, y - 1, z)
}
