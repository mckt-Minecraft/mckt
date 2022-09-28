package io.github.gaming32.mckt.objects

enum class BlockRotation {
    NONE,
    CLOCKWISE_90,
    CLOCKWISE_180,
    COUNTERCLOCKWISE_90;

    companion object {
        fun Direction.rotate(rotation: BlockRotation) =
            if (axis == Axis.Y) {
                this
            } else when (rotation) {
                CLOCKWISE_90 -> rotateYClockwise()
                CLOCKWISE_180 -> opposite
                COUNTERCLOCKWISE_90 -> rotateYCounterclockwise()
                else -> this
            }
    }

    operator fun plus(other: BlockRotation) = when (other) {
        CLOCKWISE_180 -> when (this) {
            NONE -> CLOCKWISE_180
            CLOCKWISE_90 -> COUNTERCLOCKWISE_90
            CLOCKWISE_180 -> NONE
            COUNTERCLOCKWISE_90 -> CLOCKWISE_90
        }
        COUNTERCLOCKWISE_90 -> when (this) {
            NONE -> COUNTERCLOCKWISE_90
            CLOCKWISE_90 -> NONE
            CLOCKWISE_180 -> CLOCKWISE_90
            COUNTERCLOCKWISE_90 -> CLOCKWISE_180
        }
        CLOCKWISE_90 -> when (this) {
            NONE -> CLOCKWISE_90
            CLOCKWISE_90 -> CLOCKWISE_180
            CLOCKWISE_180 -> COUNTERCLOCKWISE_90
            COUNTERCLOCKWISE_90 -> NONE
        }
        else -> this
    }
}
