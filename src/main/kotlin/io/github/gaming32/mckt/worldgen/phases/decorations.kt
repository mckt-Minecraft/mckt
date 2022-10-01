package io.github.gaming32.mckt.worldgen.phases

import io.github.gaming32.mckt.BlockAccess
import io.github.gaming32.mckt.Blocks
import io.github.gaming32.mckt.objects.BlockState
import io.github.gaming32.mckt.worldgen.DefaultWorldGenerator
import io.github.gaming32.mckt.worldgen.WorldgenPhase
import io.github.gaming32.mckt.worldgen.noise.PerlinNoise
import kotlin.random.Random

class TreeDecorationPhase(generator: DefaultWorldGenerator) : WorldgenPhase(generator) {
    companion object {
        private const val REQUIREMENT = 0.2
        private const val REQUIREMENT_2 = 0.4
        private const val REQUIREMENT_3 = 0.6
        private const val FLIP_CONSTANT = 4009383296558120008L
        private const val SCALE = 200.0
        private const val MAX_TRIES = 40
    }

    private val perlin = PerlinNoise(generator.seed xor FLIP_CONSTANT)

    override fun generateChunk(chunk: BlockAccess, chunkX: Int, chunkZ: Int, rand: Random) {
        val cx = chunkX shl 4
        val cz = chunkZ shl 4
        val noise = perlin.noise2d(cx / SCALE, cz / SCALE)
        if (noise < REQUIREMENT) return
        val offsetX = rand.nextInt(12)
        val offsetZ = rand.nextInt(12)
        generateTree(
            chunk, rand,
            offsetX,
            generator.groundPhase.getHeight(cx + offsetX + 2, cz + offsetZ + 2) + 1,
            offsetZ
        )
        if (noise < REQUIREMENT_2) return
        var offsetX2: Int
        var i = 0
        do {
            offsetX2 = rand.nextInt(12)
            if (i++ > MAX_TRIES) return
        } while (offsetX2 in offsetX - 4..offsetX + 4)
        var offsetZ2: Int
        i = 0
        do {
            offsetZ2 = rand.nextInt(12)
            if (i++ > MAX_TRIES) return
        } while (offsetZ2 in offsetZ - 4..offsetZ + 4)
        generateTree(
            chunk, rand,
            offsetX2,
            generator.groundPhase.getHeight(cx + offsetX2 + 2, cz + offsetZ2 + 2) + 1,
            offsetZ2
        )
        if (noise < REQUIREMENT_3) return
        var offsetX3: Int
        i = 0
        do {
            offsetX3 = rand.nextInt(12)
            if (i++ > MAX_TRIES) return
        } while (offsetX3 in offsetX - 4..offsetX + 4 || offsetX3 in offsetX2 - 4..offsetX2 + 4)
        var offsetZ3: Int
        i = 0
        do {
            offsetZ3 = rand.nextInt(12)
            if (i++ > MAX_TRIES) return
        } while (offsetZ3 in offsetZ - 4..offsetZ + 4 || offsetZ3 in offsetZ2 - 4..offsetZ2 + 4)
        generateTree(
            chunk, rand,
            offsetX3,
            generator.groundPhase.getHeight(cx + offsetX3 + 2, cz + offsetZ3 + 2) + 1,
            offsetZ3
        )
    }

    private fun generateTree(into: BlockAccess, rand: Random, x: Int, y: Int, z: Int) {
        val birch = rand.nextInt(15) == 0
        generateTree(
            into, rand, x, y, z,
            if (birch) Blocks.BIRCH_LOG else Blocks.OAK_LOG,
            if (birch) Blocks.BIRCH_LEAVES else Blocks.OAK_LEAVES
        )
    }
}

fun generateTree(into: BlockAccess, rand: Random, x: Int, y: Int, z: Int, trunk: BlockState, leaves: BlockState) {
    val endY = rand.nextInt(y + 3, y + 7)
    for (oy in y..endY) {
        into.setBlockImmediate(x + 2, oy, z + 2, trunk)
    }
    for (oy in endY - 2 until endY) {
        for (ox in 0 until 5) {
            for (oz in 0 until 5) {
                if (
                    (ox == 2 && oz == 2) ||
                    (ox == 0 && oz == 0 && rand.nextBoolean()) ||
                    (ox == 4 && oz == 0 && rand.nextBoolean()) ||
                    (ox == 0 && oz == 4 && rand.nextBoolean()) ||
                    (ox == 4 && oz == 4 && rand.nextBoolean())
                ) continue
                into.setBlockImmediate(x + ox, oy, z + oz, leaves)
            }
        }
    }
    into.setBlockImmediate(x + 1, endY, z + 2, leaves)
    into.setBlockImmediate(x + 3, endY, z + 2, leaves)
    into.setBlockImmediate(x + 2, endY, z + 1, leaves)
    into.setBlockImmediate(x + 2, endY, z + 3, leaves)
    if (rand.nextBoolean()) into.setBlockImmediate(x + 1, endY, z + 1, leaves)
    if (rand.nextBoolean()) into.setBlockImmediate(x + 3, endY, z + 1, leaves)
    if (rand.nextBoolean()) into.setBlockImmediate(x + 1, endY, z + 3, leaves)
    if (rand.nextBoolean()) into.setBlockImmediate(x + 3, endY, z + 3, leaves)
    into.setBlockImmediate(x + 1, endY + 1, z + 2, leaves)
    into.setBlockImmediate(x + 3, endY + 1, z + 2, leaves)
    into.setBlockImmediate(x + 2, endY + 1, z + 1, leaves)
    into.setBlockImmediate(x + 2, endY + 1, z + 3, leaves)
    into.setBlockImmediate(x + 2, endY + 1, z + 2, leaves)
    into.setBlockImmediate(x + 2, y - 1, z + 2, Blocks.DIRT)
}
