package io.github.gaming32.mckt.world.gen.defaultgen.phases

import io.github.gaming32.mckt.world.BlockAccess
import io.github.gaming32.mckt.world.Blocks
import io.github.gaming32.mckt.world.gen.defaultgen.DefaultWorldGenerator
import io.github.gaming32.mckt.world.gen.defaultgen.WorldgenPhase

class BottomPhase(generator: DefaultWorldGenerator) : WorldgenPhase(generator) {
    companion object {
        private const val PHASE_SEED = 9195089414756219909L
    }

    override fun generateChunk(chunk: BlockAccess, chunkX: Int, chunkZ: Int) {
        val rand = generator.getRandom(chunkX, chunkZ, PHASE_SEED)
        repeat(16) { x ->
            repeat(16) { z ->
                chunk.setBlockImmediate(x, -2032, z, Blocks.BEDROCK)
                chunk.setBlock3Way(x, -2031, z, rand.nextInt(4) != 0)
                chunk.setBlock3Way(x, -2030, z, rand.nextBoolean())
                chunk.setBlock3Way(x, -2029, z, rand.nextInt(4) == 0)
                for (y in -2028..-2017) {
                    if (chunk.getBlockImmediate(x, y, z) == Blocks.AIR) {
                        chunk.setBlockImmediate(x, y, z, Blocks.LAVA)
                    }
                }
            }
        }
    }

    private fun BlockAccess.setBlock3Way(x: Int, y: Int, z: Int, yes: Boolean) {
        if (yes) {
            setBlockImmediate(x, y, z, Blocks.BEDROCK)
        } else if (getBlockImmediate(x, y, z) == Blocks.AIR) {
            setBlockImmediate(x, y, z, Blocks.LAVA)
        }
    }
}
