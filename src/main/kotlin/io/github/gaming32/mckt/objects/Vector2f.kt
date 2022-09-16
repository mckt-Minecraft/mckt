package io.github.gaming32.mckt.objects

import kotlin.math.sqrt

data class Vector2f(val x: Float, val y: Float) {
    companion object Consts {
        val ZERO = Vector2f(0f, 0f)
        val UP = Vector2f(0f, 1f)
        val DOWN = Vector2f(0f, -1f)
        val LEFT = Vector2f(-1f, 0f)
        val RIGHT = Vector2f(1f, 0f)
    }

    operator fun plus(other: Vector2f) = Vector2f(x + other.x, y + other.y)
    operator fun minus(other: Vector2f) = Vector2f(x - other.x, y - other.y)
    operator fun times(other: Float) = Vector2f(x * other, y * other)
    operator fun div(other: Float) = Vector2f(x / other, y / other)

    val magnitudeSquared get() = x * x + y * y
    val magnitude get() = sqrt(magnitudeSquared)
    val normalized get() = magnitude.let { if (it == 0f) ZERO else this / it }
}
