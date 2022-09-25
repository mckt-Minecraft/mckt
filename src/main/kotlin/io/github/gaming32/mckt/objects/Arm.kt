package io.github.gaming32.mckt.objects

enum class Arm {
    LEFT, RIGHT;

    val opposite get() = Arm.values()[(ordinal + 1) and 2]
}
