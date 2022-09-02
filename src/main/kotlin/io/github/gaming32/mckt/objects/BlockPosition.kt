package io.github.gaming32.mckt.objects

data class BlockPosition(val x: Int, val y: Int, val z: Int) {
    companion object {
        fun decodeFromLong(value: Long) = BlockPosition(
            (value shr 38).toInt(),
            (value shl 52 shr 52).toInt(),
            (value shl 26 shr 38).toInt()
        )
    }

    fun encodeToLong() =
        (x.toLong() and 0x3FFFFFFL shl 38) or (z.toLong() and 0x3FFFFFFL shl 12) or (y.toLong() and 0xFFFL)
}
