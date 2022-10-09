package io.github.gaming32.mckt.objects

import io.github.gaming32.mckt.PlayClient
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

enum class Direction(
    val axis: Axis,
    val direction: Int
) {
    DOWN(Axis.Y, -1),
    UP(Axis.Y, 1),
    NORTH(Axis.Z, -1),
    SOUTH(Axis.Z, 1),
    WEST(Axis.X, -1),
    EAST(Axis.X, 1);

    companion object {
        private val HORIZONTAL = arrayOf(SOUTH, WEST, NORTH, EAST)

        fun fromHorizontal(id: Int) = HORIZONTAL[abs(id % HORIZONTAL.size)]

        fun fromYaw(yaw: Float) = fromHorizontal(floor(yaw / 90f + 0.5f).toInt() and 3)

        fun getEntityFacingOrder(client: PlayClient): Array<Direction> {
            val f = client.data.pitch * (Math.PI / 180.0).toFloat()
            val g = -client.data.yaw * (Math.PI / 180.0).toFloat()
            val h = sin(f)
            val i = cos(f)
            val j = sin(g)
            val k = cos(g)
            val bl = j > 0.0f
            val bl2 = h < 0.0f
            val bl3 = k > 0.0f
            val l = if (bl) j else -j
            val m = if (bl2) -h else h
            val n = if (bl3) k else -k
            val o = l * i
            val p = n * i
            val direction = if (bl) EAST else WEST
            val direction2 = if (bl2) UP else DOWN
            val direction3 = if (bl3) SOUTH else NORTH
            return if (l > n) {
                if (m > o) {
                    listClosest(direction2, direction, direction3)
                } else {
                    if (p > m) listClosest(direction, direction3, direction2) else listClosest(
                        direction,
                        direction2,
                        direction3
                    )
                }
            } else if (m > p) {
                listClosest(direction2, direction3, direction)
            } else {
                if (o > m) listClosest(direction3, direction, direction2) else listClosest(
                    direction3,
                    direction2,
                    direction
                )
            }
        }

        private fun listClosest(first: Direction, second: Direction, third: Direction): Array<Direction> {
            return arrayOf(first, second, third, third.opposite, second.opposite, first.opposite)
        }
    }

    val vector get() = axis.direction(direction)
    val opposite get() = values()[if ((ordinal and 1) == 0) ordinal + 1 else ordinal - 1]

    fun rotateClockwise(axis: Axis) = when (axis) {
        Axis.X -> if (this != WEST && this != EAST) rotateXClockwise() else this
        Axis.Z -> if (this != UP && this != DOWN) rotateYClockwise() else this
        Axis.Y -> if (this != NORTH && this != SOUTH) rotateZClockwise() else this
    }

    fun rotateCounterclockwise(axis: Axis) = when (axis) {
        Axis.X -> if (this != WEST && this != EAST) rotateXCounterclockwise() else this
        Axis.Z -> if (this != UP && this != DOWN) rotateYCounterclockwise() else this
        Axis.Y -> if (this != NORTH && this != SOUTH) rotateZCounterclockwise() else this
    }

    fun rotateXClockwise() = when (this) {
        DOWN -> SOUTH
        UP -> NORTH
        NORTH -> DOWN
        SOUTH -> UP
        else -> throw IllegalArgumentException("Can't rotate $this on X-axis")
    }

    fun rotateXCounterclockwise() = when (this) {
        DOWN -> NORTH
        UP -> SOUTH
        NORTH -> UP
        SOUTH -> DOWN
        else -> throw IllegalArgumentException("Can't rotate $this on X-axis")
    }

    fun rotateYClockwise() = when (this) {
        NORTH -> EAST
        SOUTH -> WEST
        WEST -> NORTH
        EAST -> SOUTH
        else -> throw IllegalArgumentException("Can't rotate $this on Y-axis")
    }

    fun rotateYCounterclockwise() = when (this) {
        NORTH -> WEST
        SOUTH -> EAST
        WEST -> SOUTH
        EAST -> NORTH
        else -> throw IllegalArgumentException("Can't rotate $this on Y-axis")
    }

    fun rotateZClockwise() = when (this) {
        DOWN -> WEST
        UP -> EAST
        WEST -> UP
        EAST -> DOWN
        else -> throw IllegalArgumentException("Can't rotate $this on Z-axis")
    }

    fun rotateZCounterclockwise() = when (this) {
        DOWN -> EAST
        UP -> WEST
        WEST -> DOWN
        EAST -> UP
        else -> throw IllegalArgumentException("Can't rotate $this on Z-axis")
    }
}
