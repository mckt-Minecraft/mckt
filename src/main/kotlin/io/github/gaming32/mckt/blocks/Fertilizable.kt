package io.github.gaming32.mckt.blocks

import io.github.gaming32.mckt.objects.BlockPosition
import io.github.gaming32.mckt.objects.BlockState
import io.github.gaming32.mckt.world.World
import kotlin.random.Random

interface Fertilizable {
    suspend fun isFertilizable(world: World, location: BlockPosition, state: BlockState): Boolean

    suspend fun canGrow(world: World, rand: Random, location: BlockPosition, state: BlockState): Boolean

    suspend fun grow(world: World, rand: Random, location: BlockPosition, state: BlockState)
}
