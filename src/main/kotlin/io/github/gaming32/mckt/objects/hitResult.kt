package io.github.gaming32.mckt.objects

sealed class HitResult(val position: Vector3d) {
    enum class Type {
        MISS, BLOCK, ENTITY
    }

    abstract val type: Type

    val missed get() = type == Type.MISS
}

class BlockHitResult(
    position: Vector3d,
    val location: BlockPosition,
    val side: Direction,
    missed: Boolean = false,
    val insideBlock: Boolean = false
) : HitResult(position) {
    override val type = if (missed) Type.MISS else Type.BLOCK
}
