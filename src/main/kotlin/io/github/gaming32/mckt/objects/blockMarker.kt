package io.github.gaming32.mckt.objects

import io.github.gaming32.mckt.data.writeBlockPosition
import io.github.gaming32.mckt.data.writeInt
import io.github.gaming32.mckt.data.writeString
import java.io.OutputStream

typealias SingleWritableBlockMarker = (out: OutputStream, baseTime: Long) -> Unit

sealed interface WritableBlockMarker {
    val color: Int
    val expiration: Long
    val writers: Iterable<SingleWritableBlockMarker>
}

data class BlockMarker(
    val location: BlockPosition,
    override val color: Int,
    val name: String = "",
    override val expiration: Long = -1L
) : WritableBlockMarker {
    override val writers = listOf { out: OutputStream, baseTime: Long ->
        out.writeBlockPosition(location)
        out.writeInt(color)
        out.writeString(name)
        if (expiration >= 0L) {
            out.writeInt((expiration - baseTime).coerceIn(0, Int.MAX_VALUE.toLong()).toInt())
        } else {
            out.writeInt(Int.MAX_VALUE)
        }
    }
}

data class BlockBoxMarker(
    val region: BlockBox,
    override val color: Int,
    override val expiration: Long = -1L
) : WritableBlockMarker {
    override val writers = buildList {
        region.forEach { x, y, z ->
            add { out: OutputStream, baseTime: Long ->
                out.writeBlockPosition(BlockPosition(x, y, z))
                out.writeInt(color)
                out.writeString("")
                if (expiration >= 0L) {
                    out.writeInt((expiration - baseTime).coerceIn(0, Int.MAX_VALUE.toLong()).toInt())
                } else {
                    out.writeInt(Int.MAX_VALUE)
                }
            }
        }
    }
}
