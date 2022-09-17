package io.github.gaming32.mckt.objects

data class EntityDimensions(val width: Double, val height: Double) {
    fun toAabb() = AABB(-width / 2, 0.0, -width / 2, width / 2, height, width / 2)
}
