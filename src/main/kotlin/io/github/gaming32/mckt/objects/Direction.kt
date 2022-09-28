package io.github.gaming32.mckt.objects

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
