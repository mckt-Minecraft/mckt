package io.github.gaming32.mckt.worldgen.defaultgen.phases

import io.github.gaming32.mckt.BlockAccess
import io.github.gaming32.mckt.Blocks
import io.github.gaming32.mckt.objects.BlockState
import io.github.gaming32.mckt.worldgen.defaultgen.DefaultWorldGenerator
import io.github.gaming32.mckt.worldgen.defaultgen.WorldgenPhase
import io.github.gaming32.mckt.worldgen.noise.PerlinNoise
import kotlin.random.Random

class TreeDecorationPhase(generator: DefaultWorldGenerator) : WorldgenPhase(generator) {
    companion object {
        private const val REQUIREMENT = 0.2
        private const val REQUIREMENT_SCALE = 0.5 / 6
        private const val FLIP_CONSTANT = 4009383296558120008L
        private const val SCALE = 200.0
        private const val TREE_COUNT = 5
        private const val LOCATION_SEED = 7110284434172948318L
        private const val TREE_SEED = 2631182125980046953L
    }

    private val perlin = PerlinNoise(generator.seed xor FLIP_CONSTANT)

    private fun getTreeGenerations(chunkX: Int, chunkZ: Int): List<TreeGeneration> {
        val rand = generator.getRandom(chunkX, chunkZ, LOCATION_SEED)
        val cx = chunkX shl 4
        val cz = chunkZ shl 4
        val noise = perlin.noise2d(cx / SCALE, cz / SCALE)
        if (noise < REQUIREMENT) return emptyList()
        val gens = mutableListOf<TreeGeneration>()
        repeat(TREE_COUNT) { i ->
            if (noise < REQUIREMENT + REQUIREMENT_SCALE * i) return gens
            val offsetX = rand.nextInt(16)
            val offsetZ = rand.nextInt(16)
            gens.add(
                TreeGeneration.random(
                    offsetX,
                    generator.groundPhase.getHeight(cx + offsetX + 2, cz + offsetZ + 2) + 1,
                    offsetZ,
                    rand
                )
            )
        }
        return gens
    }

    fun getTreeCount(chunkX: Int, chunkZ: Int): Int {
        val cx = chunkX shl 4
        val cz = chunkZ shl 4
        val noise = perlin.noise2d(cx / SCALE, cz / SCALE)
        repeat(TREE_COUNT) { i ->
            if (noise < REQUIREMENT + REQUIREMENT_SCALE * i) return i
        }
        return TREE_COUNT
    }

    override fun generateChunk(chunk: BlockAccess, chunkX: Int, chunkZ: Int) {
        for (offsetX in -1..1) {
            for (offsetZ in -1..1) {
                val gens = getTreeGenerations(chunkX + offsetX, chunkZ + offsetZ)
                val rand = generator.getRandom(chunkX + offsetX, chunkZ + offsetZ, TREE_SEED)
                gens.forEach { generateTree(chunk, rand, it.offsetPos(offsetX * 16, 0, offsetZ * 16)) }
            }
        }
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

data class TreeGeneration(
    val x: Int, val y: Int, val z: Int,
    val trunk: BlockState, val leaves: BlockState,
    val height: Int? = null
) {
    companion object {
        fun random(x: Int, y: Int, z: Int, rand: Random): TreeGeneration {
            val birch = rand.nextInt(15) == 0
            return TreeGeneration(
                x, y, z,
                if (birch) Blocks.BIRCH_LOG else Blocks.OAK_LOG,
                if (birch) Blocks.BIRCH_LEAVES else Blocks.OAK_LEAVES,
                rand.nextInt(3, 7)
            )
        }
    }

    fun offsetPos(x: Int, y: Int, z: Int) = TreeGeneration(this.x + x, this.y + y, this.z + z, trunk, leaves, height)
}

fun generateTree(into: BlockAccess, rand: Random, x: Int, y: Int, z: Int, trunk: BlockState, leaves: BlockState) =
    generateTree(into, rand, TreeGeneration(x, y, z, trunk, leaves))

fun generateTree(into: BlockAccess, rand: Random, gen: TreeGeneration) {
    val x = gen.x
    val y = gen.y
    val z = gen.z
    val trunk = gen.trunk
    val leaves = gen.leaves
    val endY = y + (gen.height ?: rand.nextInt(3, 7))
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
