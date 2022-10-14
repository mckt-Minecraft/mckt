package io.github.gaming32.mckt.worldgen.classic

import io.github.gaming32.mckt.Blocks

open class MobSpawnerBase {
    companion object {
        private val rainforest = MobSpawnerBase().func4079(588342).setBiomeName("Rainforest").func4080(2094168)
        private val swampland: MobSpawnerBase = MobSpawnerSwamp().func4079(522674).setBiomeName("Swampland").func4080(9154376)
        private val seasonalForest = MobSpawnerBase().func4079(10215459).setBiomeName("Seasonal Forest")
        private val forest = MobSpawnerBase().func4079(353825).setBiomeName("Forest").func4080(5159473)
        val savanna: MobSpawnerBase = MobSpawnerDesert().func4079(14278691).setBiomeName("Savanna")
        private val shrubland = MobSpawnerBase().func4079(10595616).setBiomeName("Shrubland")
        val taiga = MobSpawnerBase().func4079(3060051).setBiomeName("Taiga").func4083().func4080(8107825)
        val desert: MobSpawnerBase = MobSpawnerDesert().func4079(16421912).setBiomeName("Desert")
        val plains: MobSpawnerBase = MobSpawnerDesert().func4079(16767248).setBiomeName("Plains")
        private val iceDesert: MobSpawnerBase = MobSpawnerDesert().func4079(16772499).setBiomeName("Ice Desert").func4083().func4080(12899129)
        private val tundra = MobSpawnerBase().func4079(5762041).setBiomeName("Tundra").func4083().func4080(12899129)
//        val hell: MobSpawnerBase = MobSpawnerHell().func_4079_b(16711680).setBiomeName("Hell")

        private val biomeLookupTable = arrayOfNulls<MobSpawnerBase>(4096)

        init {
            generateBiomeLookup()
        }

        private fun generateBiomeLookup() {
            for (var0 in 0..63) {
                for (var1 in 0..63) {
                    biomeLookupTable[var0 + var1 * 64] = getBiome(var0.toFloat() / 63.0f, var1.toFloat() / 63.0f)
                }
            }
            desert.fillerBlock = Blocks.SAND
            desert.topBlock = desert.fillerBlock
            iceDesert.fillerBlock = Blocks.SAND
            iceDesert.topBlock = iceDesert.fillerBlock
        }

        fun getBiomeFromLookup(var0: Double, var2: Double): MobSpawnerBase {
            val var4 = (var0 * 63.0).toInt()
            val var5 = (var2 * 63.0).toInt()
            return biomeLookupTable[var4 + var5 * 64] ?: plains
        }

        private fun getBiome(var0: Float, var1: Float): MobSpawnerBase {
            var var1tmp = var1
            var1tmp *= var0
            return if (var0 < 0.1f) {
                tundra
            } else if (var1tmp < 0.2f) {
                if (var0 < 0.5f) {
                    tundra
                } else {
                    if (var0 < 0.95f) savanna else desert
                }
            } else if (var1tmp > 0.5f && var0 < 0.7f) {
                swampland
            } else if (var0 < 0.5f) {
                taiga
            } else if (var0 < 0.97f) {
                if (var1tmp < 0.35f) shrubland else forest
            } else if (var1tmp < 0.45f) {
                plains
            } else {
                if (var1tmp < 0.9f) seasonalForest else rainforest
            }
        }
    }
    private var biomeName: String? = null
    private var field6162 = 0
    var topBlock = Blocks.GRASS_BLOCK
    var fillerBlock = Blocks.DIRT
    private var field6161 = 5169201
//    protected var biomeMonsters = arrayOf<Class<*>>(
//        EntitySpider::class.java,
//        EntityZombie::class.java,
//        EntitySkeleton::class.java,
//        EntityCreeper::class.java
//    )
//    protected var biomeCreatures = arrayOf<Class<*>>(
//        EntitySheep::class.java,
//        EntityPig::class.java,
//        EntityChicken::class.java,
//        EntityCow::class.java
//    )

    protected fun func4083(): MobSpawnerBase {
        return this
    }

    protected fun setBiomeName(var1: String?): MobSpawnerBase {
        biomeName = var1
        return this
    }

    protected fun func4080(var1: Int): MobSpawnerBase {
        field6161 = var1
        return this
    }

    protected fun func4079(var1: Int): MobSpawnerBase {
        field6162 = var1
        return this
    }

//    fun getEntitiesForType(var1: EnumCreatureType): Array<Class<*>>? {
//        return if (var1 === EnumCreatureType.monster) {
//            biomeMonsters
//        } else {
//            if (var1 === EnumCreatureType.creature) biomeCreatures else null
//        }
//    }
}
