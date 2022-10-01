package io.github.gaming32.mckt.blocks

import io.github.gaming32.mckt.Blocks
import io.github.gaming32.mckt.GlobalPalette.DEFAULT_BLOCKSTATES
import io.github.gaming32.mckt.World
import io.github.gaming32.mckt.objects.BlockPosition
import io.github.gaming32.mckt.objects.BlockState
import io.github.gaming32.mckt.objects.Identifier
import io.github.gaming32.mckt.worldgen.phases.generateTree
import kotlin.random.Random

object SaplingBlockHandler : BlockHandler(), Fertilizable {
    override suspend fun isFertilizable(world: World, location: BlockPosition, state: BlockState) = true

    override suspend fun canGrow(world: World, rand: Random, location: BlockPosition, state: BlockState) =
        rand.nextFloat() < 0.45f

    override suspend fun grow(world: World, rand: Random, location: BlockPosition, state: BlockState) {
        if (state["stage"] == "0") {
            world.setBlock(location, state.with("stage", "1"))
            return
        }

        val baseBlock = state.blockId.toShortString().removeSuffix("sapling")
        generateTree(
            world, rand, location.x - 2, location.y, location.z - 2,
            DEFAULT_BLOCKSTATES[Identifier.parse(baseBlock + "log")] ?: Blocks.OAK_LOG,
            DEFAULT_BLOCKSTATES[Identifier.parse(baseBlock + "leaves")] ?: Blocks.OAK_LEAVES
        )
    }
}
