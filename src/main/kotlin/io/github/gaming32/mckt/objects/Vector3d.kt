package io.github.gaming32.mckt.objects

import kotlin.math.floor
import kotlin.math.sqrt

data class Vector3d(val x: Double, val y: Double, val z: Double) {
    companion object Consts {
        val ZERO = Vector3d(0.0, 0.0, 0.0)
        val UP = Vector3d(0.0, 1.0, 0.0)
        val DOWN = Vector3d(0.0, -1.0, 0.0)
        val NORTH = Vector3d(0.0, 0.0, -1.0)
        val SOUTH = Vector3d(0.0, 0.0, 1.0)
        val WEST = Vector3d(-1.0, 0.0, 0.0)
        val EAST = Vector3d(1.0, 0.0, 0.0)
    }

    operator fun plus(other: Vector3d) = Vector3d(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vector3d) = Vector3d(x - other.x, y - other.y, z - other.z)
    operator fun times(other: Double) = Vector3d(x * other, y * other, z * other)
    operator fun div(other: Double) = Vector3d(x / other, y / other, z / other)

    val magnitudeSquared get() = x * x + y * y + z * z
    val magnitude get() = sqrt(magnitudeSquared)
    val normalized get() = magnitude.let { if (it == 0.0) ZERO else this / it }

    infix fun cross(other: Vector3d) = Vector3d(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )

    fun toBlockPosition() = BlockPosition(floor(x).toInt(), floor(y).toInt(), floor(z).toInt())

    fun distanceTo(other: Vector3d) = sqrt(distanceToSquared(other))
    fun distanceToSquared(other: Vector3d) =
        (x - other.x) * (x - other.x) +
        (y - other.y) * (y - other.y) +
        (z - other.z) * (z - other.z)

    override fun toString() = "$x, $y, $z"
}
