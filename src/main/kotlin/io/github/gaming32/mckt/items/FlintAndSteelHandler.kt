package io.github.gaming32.mckt.items

import io.github.gaming32.mckt.Blocks
import io.github.gaming32.mckt.objects.Direction
import io.github.gaming32.mckt.objects.Identifier

object FlintAndSteelHandler : ItemEventHandler {
    private val burnChancesInternal = mutableMapOf<Identifier, Int>()
    private val spreadChancesInternal = mutableMapOf<Identifier, Int>()
    val burnChances: Map<Identifier, Int> = burnChancesInternal
    val spreadChances: Map<Identifier, Int> = spreadChancesInternal

    fun registerFlammable(block: Identifier, burnChance: Int, spreadChance: Int) {
        burnChancesInternal[block] = burnChance
        spreadChancesInternal[block] = spreadChance
    }

    fun isFlammable(block: Identifier) = block in burnChancesInternal

    init {
        registerFlammable(Identifier("oak_planks"), 5, 20)
        registerFlammable(Identifier("spruce_planks"), 5, 20)
        registerFlammable(Identifier("birch_planks"), 5, 20)
        registerFlammable(Identifier("jungle_planks"), 5, 20)
        registerFlammable(Identifier("acacia_planks"), 5, 20)
        registerFlammable(Identifier("dark_oak_planks"), 5, 20)
        registerFlammable(Identifier("mangrove_planks"), 5, 20)
        registerFlammable(Identifier("oak_slab"), 5, 20)
        registerFlammable(Identifier("spruce_slab"), 5, 20)
        registerFlammable(Identifier("birch_slab"), 5, 20)
        registerFlammable(Identifier("jungle_slab"), 5, 20)
        registerFlammable(Identifier("acacia_slab"), 5, 20)
        registerFlammable(Identifier("dark_oak_slab"), 5, 20)
        registerFlammable(Identifier("mangrove_slab"), 5, 20)
        registerFlammable(Identifier("oak_fence_gate"), 5, 20)
        registerFlammable(Identifier("spruce_fence_gate"), 5, 20)
        registerFlammable(Identifier("birch_fence_gate"), 5, 20)
        registerFlammable(Identifier("jungle_fence_gate"), 5, 20)
        registerFlammable(Identifier("acacia_fence_gate"), 5, 20)
        registerFlammable(Identifier("dark_oak_fence_gate"), 5, 20)
        registerFlammable(Identifier("mangrove_fence_gate"), 5, 20)
        registerFlammable(Identifier("oak_fence"), 5, 20)
        registerFlammable(Identifier("spruce_fence"), 5, 20)
        registerFlammable(Identifier("birch_fence"), 5, 20)
        registerFlammable(Identifier("jungle_fence"), 5, 20)
        registerFlammable(Identifier("acacia_fence"), 5, 20)
        registerFlammable(Identifier("dark_oak_fence"), 5, 20)
        registerFlammable(Identifier("mangrove_fence"), 5, 20)
        registerFlammable(Identifier("oak_stairs"), 5, 20)
        registerFlammable(Identifier("birch_stairs"), 5, 20)
        registerFlammable(Identifier("spruce_stairs"), 5, 20)
        registerFlammable(Identifier("jungle_stairs"), 5, 20)
        registerFlammable(Identifier("acacia_stairs"), 5, 20)
        registerFlammable(Identifier("dark_oak_stairs"), 5, 20)
        registerFlammable(Identifier("mangrove_stairs"), 5, 20)
        registerFlammable(Identifier("oak_log"), 5, 5)
        registerFlammable(Identifier("spruce_log"), 5, 5)
        registerFlammable(Identifier("birch_log"), 5, 5)
        registerFlammable(Identifier("jungle_log"), 5, 5)
        registerFlammable(Identifier("acacia_log"), 5, 5)
        registerFlammable(Identifier("dark_oak_log"), 5, 5)
        registerFlammable(Identifier("mangrove_log"), 5, 5)
        registerFlammable(Identifier("stripped_oak_log"), 5, 5)
        registerFlammable(Identifier("stripped_spruce_log"), 5, 5)
        registerFlammable(Identifier("stripped_birch_log"), 5, 5)
        registerFlammable(Identifier("stripped_jungle_log"), 5, 5)
        registerFlammable(Identifier("stripped_acacia_log"), 5, 5)
        registerFlammable(Identifier("stripped_dark_oak_log"), 5, 5)
        registerFlammable(Identifier("stripped_mangrove_log"), 5, 5)
        registerFlammable(Identifier("stripped_oak_wood"), 5, 5)
        registerFlammable(Identifier("stripped_spruce_wood"), 5, 5)
        registerFlammable(Identifier("stripped_birch_wood"), 5, 5)
        registerFlammable(Identifier("stripped_jungle_wood"), 5, 5)
        registerFlammable(Identifier("stripped_acacia_wood"), 5, 5)
        registerFlammable(Identifier("stripped_dark_oak_wood"), 5, 5)
        registerFlammable(Identifier("stripped_mangrove_wood"), 5, 5)
        registerFlammable(Identifier("oak_wood"), 5, 5)
        registerFlammable(Identifier("spruce_wood"), 5, 5)
        registerFlammable(Identifier("birch_wood"), 5, 5)
        registerFlammable(Identifier("jungle_wood"), 5, 5)
        registerFlammable(Identifier("acacia_wood"), 5, 5)
        registerFlammable(Identifier("dark_oak_wood"), 5, 5)
        registerFlammable(Identifier("mangrove_wood"), 5, 5)
        registerFlammable(Identifier("mangrove_roots"), 5, 20)
        registerFlammable(Identifier("oak_leaves"), 30, 60)
        registerFlammable(Identifier("spruce_leaves"), 30, 60)
        registerFlammable(Identifier("birch_leaves"), 30, 60)
        registerFlammable(Identifier("jungle_leaves"), 30, 60)
        registerFlammable(Identifier("acacia_leaves"), 30, 60)
        registerFlammable(Identifier("dark_oak_leaves"), 30, 60)
        registerFlammable(Identifier("mangrove_leaves"), 30, 60)
        registerFlammable(Identifier("bookshelf"), 30, 20)
        registerFlammable(Identifier("tnt"), 15, 100)
        registerFlammable(Identifier("grass"), 60, 100)
        registerFlammable(Identifier("fern"), 60, 100)
        registerFlammable(Identifier("dead_bush"), 60, 100)
        registerFlammable(Identifier("sunflower"), 60, 100)
        registerFlammable(Identifier("lilac"), 60, 100)
        registerFlammable(Identifier("rose_bush"), 60, 100)
        registerFlammable(Identifier("peony"), 60, 100)
        registerFlammable(Identifier("tall_grass"), 60, 100)
        registerFlammable(Identifier("large_fern"), 60, 100)
        registerFlammable(Identifier("dandelion"), 60, 100)
        registerFlammable(Identifier("poppy"), 60, 100)
        registerFlammable(Identifier("blue_orchid"), 60, 100)
        registerFlammable(Identifier("allium"), 60, 100)
        registerFlammable(Identifier("azure_bluet"), 60, 100)
        registerFlammable(Identifier("red_tulip"), 60, 100)
        registerFlammable(Identifier("orange_tulip"), 60, 100)
        registerFlammable(Identifier("white_tulip"), 60, 100)
        registerFlammable(Identifier("pink_tulip"), 60, 100)
        registerFlammable(Identifier("oxeye_daisy"), 60, 100)
        registerFlammable(Identifier("cornflower"), 60, 100)
        registerFlammable(Identifier("lily_of_the_valley"), 60, 100)
        registerFlammable(Identifier("wither_rose"), 60, 100)
        registerFlammable(Identifier("white_wool"), 30, 60)
        registerFlammable(Identifier("orange_wool"), 30, 60)
        registerFlammable(Identifier("magenta_wool"), 30, 60)
        registerFlammable(Identifier("light_blue_wool"), 30, 60)
        registerFlammable(Identifier("yellow_wool"), 30, 60)
        registerFlammable(Identifier("lime_wool"), 30, 60)
        registerFlammable(Identifier("pink_wool"), 30, 60)
        registerFlammable(Identifier("gray_wool"), 30, 60)
        registerFlammable(Identifier("light_gray_wool"), 30, 60)
        registerFlammable(Identifier("cyan_wool"), 30, 60)
        registerFlammable(Identifier("purple_wool"), 30, 60)
        registerFlammable(Identifier("blue_wool"), 30, 60)
        registerFlammable(Identifier("brown_wool"), 30, 60)
        registerFlammable(Identifier("green_wool"), 30, 60)
        registerFlammable(Identifier("red_wool"), 30, 60)
        registerFlammable(Identifier("black_wool"), 30, 60)
        registerFlammable(Identifier("vine"), 15, 100)
        registerFlammable(Identifier("coal_block"), 5, 5)
        registerFlammable(Identifier("hay_block"), 60, 20)
        registerFlammable(Identifier("target"), 15, 20)
        registerFlammable(Identifier("white_carpet"), 60, 20)
        registerFlammable(Identifier("orange_carpet"), 60, 20)
        registerFlammable(Identifier("magenta_carpet"), 60, 20)
        registerFlammable(Identifier("light_blue_carpet"), 60, 20)
        registerFlammable(Identifier("yellow_carpet"), 60, 20)
        registerFlammable(Identifier("lime_carpet"), 60, 20)
        registerFlammable(Identifier("pink_carpet"), 60, 20)
        registerFlammable(Identifier("gray_carpet"), 60, 20)
        registerFlammable(Identifier("light_gray_carpet"), 60, 20)
        registerFlammable(Identifier("cyan_carpet"), 60, 20)
        registerFlammable(Identifier("purple_carpet"), 60, 20)
        registerFlammable(Identifier("blue_carpet"), 60, 20)
        registerFlammable(Identifier("brown_carpet"), 60, 20)
        registerFlammable(Identifier("green_carpet"), 60, 20)
        registerFlammable(Identifier("red_carpet"), 60, 20)
        registerFlammable(Identifier("black_carpet"), 60, 20)
        registerFlammable(Identifier("dried_kelp_block"), 30, 60)
        registerFlammable(Identifier("bamboo"), 60, 60)
        registerFlammable(Identifier("scaffolding"), 60, 60)
        registerFlammable(Identifier("lectern"), 30, 20)
        registerFlammable(Identifier("composter"), 5, 20)
        registerFlammable(Identifier("sweet_berry_bush"), 60, 100)
        registerFlammable(Identifier("beehive"), 5, 20)
        registerFlammable(Identifier("bee_nest"), 30, 20)
        registerFlammable(Identifier("azalea_leaves"), 30, 60)
        registerFlammable(Identifier("flowering_azalea_leaves"), 30, 60)
        registerFlammable(Identifier("cave_vines"), 15, 60)
        registerFlammable(Identifier("cave_vines_plant"), 15, 60)
        registerFlammable(Identifier("spore_blossom"), 60, 100)
        registerFlammable(Identifier("azalea"), 30, 60)
        registerFlammable(Identifier("flowering_azalea"), 30, 60)
        registerFlammable(Identifier("big_dripleaf"), 60, 100)
        registerFlammable(Identifier("big_dripleaf_stem"), 60, 100)
        registerFlammable(Identifier("small_dripleaf"), 60, 100)
        registerFlammable(Identifier("hanging_roots"), 30, 60)
        registerFlammable(Identifier("glow_lichen"), 15, 100)
    }

    override suspend fun useOnBlock(event: ItemEventHandler.BlockUseEvent): ItemEventHandler.Result {
        if (event.world.getBlock(event.location) == Blocks.AIR) return ItemEventHandler.Result.PASS
        val placeAt = event.location + event.face.vector
        var canPlace = false
        val properties = Blocks.FIRE.properties.toMutableMap()
        if (event.world.getBlock(placeAt.down()) != Blocks.AIR) {
            canPlace = true
        } else {
            for (direction in Direction.values()) {
                if (direction == Direction.DOWN) continue
                val block = event.world.getBlock(placeAt + direction.vector)?.blockId
                if (block != null && isFlammable(block)) {
                    properties[direction.name.lowercase()] = "true"
                    canPlace = true
                }
            }
        }
        return if (canPlace) {
            event.server.setBlock(event.location + event.face.vector, Blocks.FIRE.with(properties).canonicalize())
            ItemEventHandler.Result.SUCCESS
        } else {
            ItemEventHandler.Result.PASS
        }
    }
}
