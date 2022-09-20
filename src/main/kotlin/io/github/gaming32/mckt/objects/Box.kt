package io.github.gaming32.mckt.objects

data class Box(
    val minX: Double,
    val minY: Double,
    val minZ: Double,
    val maxX: Double,
    val maxY: Double,
    val maxZ: Double
) {
    operator fun plus(other: Vector3d) = offset(other.x, other.y, other.z)

    fun offset(x: Double, y: Double, z: Double) = Box(minX + x, minY + y, minZ + z, maxX + x, maxY + y, maxZ + z)

    fun contains(x: Double, y: Double, z: Double) =
        x >= minX && x < maxX && y >= minY && y < maxY && z >= minZ && z < maxZ

    operator fun contains(pos: Vector3d) = contains(pos.x, pos.y, pos.z)

    fun intersects(minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double) =
        this.minX < maxX && this.maxX > minX && this.minY < maxY &&
            this.maxY > minY && this.minZ < maxZ && this.maxZ > minZ

    fun intersects(other: Box) = intersects(other.minX, other.minY, other.minZ, other.maxX, other.maxY, other.maxZ)
}
