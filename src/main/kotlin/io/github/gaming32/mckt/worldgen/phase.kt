package io.github.gaming32.mckt.worldgen

import io.github.gaming32.mckt.WorldChunk
import java.util.*
import kotlin.random.Random

abstract class WorldgenPhase(val generator: DefaultWorldGenerator) {
    abstract fun generateChunk(chunk: WorldChunk, rand: Random)
}

abstract class HeightmappedPhase(generator: DefaultWorldGenerator) : WorldgenPhase(generator) {
    private val heightmaps = IdentityHashMap<Any?, MutableMap<Pair<Int, Int>, Int>>()

    protected abstract fun getHeight0(x: Int, z: Int, heightmap: Any?): Int

    fun getHeight(x: Int, z: Int, heightmap: Any? = null) =
        heightmaps.computeIfAbsent(heightmap) { mutableMapOf() }
            .computeIfAbsent(x to z) { (x, z) -> getHeight0(x, z, heightmap) }
}
