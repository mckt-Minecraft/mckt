package io.github.gaming32.mckt.objects

import io.github.gaming32.mckt.mod

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

        fun round(degrees: Int) = when (degrees mod 360) {
            in 0 until 45 -> NONE
            in 45 until 135 -> CLOCKWISE_90
            in 135 until 225 -> CLOCKWISE_180
            in 225 until 315 -> COUNTERCLOCKWISE_90
            in 315 until 360 -> NONE
            else -> throw AssertionError()
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
