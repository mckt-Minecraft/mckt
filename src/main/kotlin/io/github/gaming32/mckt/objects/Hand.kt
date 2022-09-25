package io.github.gaming32.mckt.objects

enum class Hand {
    MAINHAND, OFFHAND;

    val opposite get() = values()[(ordinal + 1) and 2]
}
