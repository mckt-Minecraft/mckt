package io.github.gaming32.mckt.objects

enum class Direction(val vector: BlockPosition) {
    BOTTOM(BlockPosition(0, -1, 0)),
    TOP(BlockPosition(0, 1, 0)),
    NORTH(BlockPosition(0, 0, -1)),
    SOUTH(BlockPosition(0, 0, 1)),
    WEST(BlockPosition(-1, 0, 0)),
    EAST(BlockPosition(1, 0, 0));

    val opposite get() = values()[if ((ordinal and 1) == 0) ordinal + 1 else ordinal - 1]
}
