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
}
