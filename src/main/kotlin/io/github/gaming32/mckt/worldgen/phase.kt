package io.github.gaming32.mckt.worldgen

import io.github.gaming32.mckt.WorldChunk
import kotlin.random.Random

abstract class WorldgenPhase(val generator: DefaultWorldGenerator) {
    abstract fun generateChunk(chunk: WorldChunk, rand: Random)
}

abstract class HeightmappedPhase(generator: DefaultWorldGenerator) : WorldgenPhase(generator) {
    private val heightmaps = mutableMapOf<String?, MutableMap<Pair<Int, Int>, Int>>()

    protected abstract fun getHeight0(x: Int, z: Int, heightmap: String?): Int

    fun getHeight(x: Int, z: Int, heightmapName: String? = null) =
        heightmaps.computeIfAbsent(heightmapName) { mutableMapOf() }
            .computeIfAbsent(x to z) { (x, z) -> getHeight0(x, z, heightmapName) }
}
