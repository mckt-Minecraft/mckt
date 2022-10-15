package io.github.gaming32.mckt.world.gen.classic

import io.github.gaming32.mckt.objects.BlockState
import io.github.gaming32.mckt.world.BlockAccess
import io.github.gaming32.mckt.world.Blocks

class ChunkProviderGenerate(private val world: GenInfo) {
    private val field4224 = DoubleArray(425)
    private val field698 = DoubleArray(256)
    private val field697 = DoubleArray(256)
    private val field696 = DoubleArray(256)
    private val field695 = MapGenCaves()
//    private val field707 = Array(32) { IntArray(32) }
    private val rand = JuRandom(world.seed)
    private val field705 = NoiseGeneratorOctaves(rand, 16)
    private val field704 = NoiseGeneratorOctaves(rand, 16)
    private val field703 = NoiseGeneratorOctaves(rand, 8)
    private val field702 = NoiseGeneratorOctaves(rand, 4)
    private val field701 = NoiseGeneratorOctaves(rand, 4)
    private val field715 = NoiseGeneratorOctaves(rand, 10)
    private val field714 = NoiseGeneratorOctaves(rand, 16)
//    private val field713 = NoiseGeneratorOctaves(rand, 8)
    private val biomesForGeneration = Array(256) { MobSpawnerBase.plains }
    private val field4229 = DoubleArray(425)
    private val field4228 = DoubleArray(425)
    private val field4227 = DoubleArray(425)
    private val field4226 = DoubleArray(25)
    private val field4225 = DoubleArray(25)

    fun generateChunk(chunk: BlockAccess, var1: Int, var2: Int) {
        rand.setSeed(var1.toLong() * 341873128712L + var2.toLong() * 132897987541L)
        val var3 = Array(32768) { Blocks.AIR }
        world.loadBlockGeneratorData(biomesForGeneration, var1 * 16, var2 * 16, 16, 16)
        val var5 = world.temperature
        generateTerrain(var1, var2, var3, /* biomesForGeneration, */ var5)
        replaceBlocksForBiome(var1, var2, var3, biomesForGeneration)
        field695.func667(this, world, var1, var2, var3)
        chunk.apply {
            repeat(16) { x ->
                repeat(128) { y ->
                    repeat(16) { z ->
                        setBlockImmediate(x, y, z, var3[(x shl 11) or (z shl 7) or y])
                    }
                }
            }
        }
    }

    private fun generateTerrain(var1: Int, var2: Int, var3: Array<BlockState>, /* var4: Array<MobSpawnerBase>, */ var5: DoubleArray) {
        val var6: Byte = 4
        val var7: Byte = 64
        val var8 = var6 + 1
        val var9: Byte = 17
        val var10 = var6 + 1
        this.func4058(this.field4224, var1 * var6, 0, var2 * var6, var8, var9.toInt(), var10)
        for (var11 in 0 until var6) {
            for (var12 in 0 until var6) {
                for (var13 in 0..15) {
                    val var14 = 0.125
                    var var16: Double = this.field4224[((var11 + 0) * var10 + var12 + 0) * var9 + var13 + 0]
                    var var18: Double = this.field4224[((var11 + 0) * var10 + var12 + 1) * var9 + var13 + 0]
                    var var20: Double = this.field4224[((var11 + 1) * var10 + var12 + 0) * var9 + var13 + 0]
                    var var22: Double = this.field4224[((var11 + 1) * var10 + var12 + 1) * var9 + var13 + 0]
                    val var24: Double =
                        (this.field4224[((var11 + 0) * var10 + var12 + 0) * var9 + var13 + 1] - var16) * var14
                    val var26: Double =
                        (this.field4224[((var11 + 0) * var10 + var12 + 1) * var9 + var13 + 1] - var18) * var14
                    val var28: Double =
                        (this.field4224[((var11 + 1) * var10 + var12 + 0) * var9 + var13 + 1] - var20) * var14
                    val var30: Double =
                        (this.field4224[((var11 + 1) * var10 + var12 + 1) * var9 + var13 + 1] - var22) * var14
                    for (var32 in 0..7) {
                        val var33 = 0.25
                        var var35 = var16
                        var var37 = var18
                        val var39 = (var20 - var16) * var33
                        val var41 = (var22 - var18) * var33
                        for (var43 in 0..3) {
                            var var44 = var43 + var11 * 4 shl 11 or (0 + var12 * 4 shl 7) or var13 * 8 + var32
                            val var45: Short = 128
                            val var46 = 0.25
                            var var48 = var35
                            val var50 = (var37 - var35) * var46
                            for (var52 in 0..3) {
                                val var53 = var5[(var11 * 4 + var43) * 16 + var12 * 4 + var52]
                                var var55 = Blocks.AIR
                                if (var13 * 8 + var32 < var7) {
                                    var55 = if (var53 < 0.5 && var13 * 8 + var32 >= var7 - 1) {
                                        Blocks.ICE
                                    } else {
//                                        Blocks.FLOWING_WATER
                                        Blocks.WATER
                                    }
                                }
                                if (var48 > 0.0) {
                                    var55 = Blocks.STONE
                                }
                                var3[var44] = var55
                                var44 += var45.toInt()
                                var48 += var50
                            }
                            var35 += var39
                            var37 += var41
                        }
                        var16 += var24
                        var18 += var26
                        var20 += var28
                        var22 += var30
                    }
                }
            }
        }
    }

    private fun func4058(
        var1: DoubleArray,
        var2: Int,
        var3: Int,
        var4: Int,
        var5: Int,
        var6: Int,
        var7: Int
    ) {
        require(var5 * var6 * var7 <= var1.size)
        val var8 = 684.412
        val var10 = 684.412
        val var12: DoubleArray = this.world.temperature
        val var13: DoubleArray = this.world.humidity
        this.field715.func4103(this.field4226, var2, var4, var5, var7, 1.121, 1.121)
        this.field714.func4103(this.field4225, var2, var4, var5, var7, 200.0, 200.0)
        this.field703
            .func648(
                this.field4229,
                var2.toDouble(),
                var3.toDouble(),
                var4.toDouble(),
                var5,
                var6,
                var7,
                var8 / 80.0,
                var10 / 160.0,
                var8 / 80.0
            )
        this.field705.func648(
            this.field4228,
            var2.toDouble(),
            var3.toDouble(),
            var4.toDouble(),
            var5,
            var6,
            var7,
            var8,
            var10,
            var8
        )
        this.field704.func648(
            this.field4227,
            var2.toDouble(),
            var3.toDouble(),
            var4.toDouble(),
            var5,
            var6,
            var7,
            var8,
            var10,
            var8
        )
        var var14 = 0
        var var15 = 0
        val var16 = 16 / var5
        for (var17 in 0 until var5) {
            val var18 = var17 * var16 + var16 / 2
            for (var19 in 0 until var7) {
                val var20 = var19 * var16 + var16 / 2
                val var21 = var12[var18 * 16 + var20]
                val var23 = var13[var18 * 16 + var20] * var21
                var var25 = 1.0 - var23
                var25 *= var25
                var25 *= var25
                var25 = 1.0 - var25
                var var27: Double = (this.field4226[var15] + 256.0) / 512.0
                var27 *= var25
                if (var27 > 1.0) {
                    var27 = 1.0
                }
                var var29: Double = this.field4225[var15] / 8000.0
                if (var29 < 0.0) {
                    var29 = -var29 * 0.3
                }
                var29 = var29 * 3.0 - 2.0
                if (var29 < 0.0) {
                    var29 /= 2.0
                    if (var29 < -1.0) {
                        var29 = -1.0
                    }
                    var29 /= 1.4
                    var29 /= 2.0
                    var27 = 0.0
                } else {
                    if (var29 > 1.0) {
                        var29 = 1.0
                    }
                    var29 /= 8.0
                }
                if (var27 < 0.0) {
                    var27 = 0.0
                }
                var27 += 0.5
                var29 = var29 * var6.toDouble() / 16.0
                val var31 = var6.toDouble() / 2.0 + var29 * 4.0
                ++var15
                for (var33 in 0 until var6) {
                    var var34: Double
                    var var36 = (var33.toDouble() - var31) * 12.0 / var27
                    if (var36 < 0.0) {
                        var36 *= 4.0
                    }
                    val var38: Double = this.field4228[var14] / 512.0
                    val var40: Double = this.field4227[var14] / 512.0
                    val var42: Double = (this.field4229[var14] / 10.0 + 1.0) / 2.0
                    var34 = if (var42 < 0.0) {
                        var38
                    } else if (var42 > 1.0) {
                        var40
                    } else {
                        var38 + (var40 - var38) * var42
                    }
                    var34 -= var36
                    if (var33 > var6 - 4) {
                        val var44 = ((var33 - (var6 - 4)).toFloat() / 3.0f).toDouble()
                        var34 = var34 * (1.0 - var44) + -10.0 * var44
                    }
                    var1[var14] = var34
                    ++var14
                }
            }
        }
    }

    private fun replaceBlocksForBiome(var1: Int, var2: Int, var3: Array<BlockState>, var4: Array<MobSpawnerBase>) {
        val var5: Byte = 64
        val var6 = 0.03125
        this.field702.func648(
            this.field698,
            (var1 * 16).toDouble(),
            (var2 * 16).toDouble(),
            0.0,
            16,
            16,
            1,
            var6,
            var6,
            1.0
        )
        this.field702.func648(
            this.field697,
            (var2 * 16).toDouble(),
            109.0134,
            (var1 * 16).toDouble(),
            16,
            1,
            16,
            var6,
            1.0,
            var6
        )
        this.field701
            .func648(
                this.field696,
                (var1 * 16).toDouble(),
                (var2 * 16).toDouble(),
                0.0,
                16,
                16,
                1,
                var6 * 2.0,
                var6 * 2.0,
                var6 * 2.0
            )
        for (var8 in 0..15) {
            for (var9 in 0..15) {
                val var10 = var4[var8 + var9 * 16]
                val var11: Boolean = this.field698[var8 + var9 * 16] + rand.nextDouble() * 0.2 > 0.0
                val var12: Boolean = this.field697[var8 + var9 * 16] + rand.nextDouble() * 0.2 > 3.0
                val var13 = (this.field696[var8 + var9 * 16] / 3.0 + 3.0 + rand.nextDouble() * 0.25).toInt()
                var var14 = -1
                var var15 = var10.topBlock
                var var16 = var10.fillerBlock
                for (var17 in 127 downTo 0) {
                    val var18 = (var8 * 16 + var9) * 128 + var17
                    if (var17 <= 0 + rand.nextInt(5)) {
                        var3[var18] = Blocks.BEDROCK
                    } else {
                        val var19 = var3[var18]
                        if (var19 == Blocks.AIR) {
                            var14 = -1
                        } else if (var19 == Blocks.STONE) {
                            if (var14 == -1) {
                                if (var13 <= 0) {
                                    var15 = Blocks.AIR
                                    var16 = Blocks.STONE
                                } else if (var17 >= var5 - 4 && var17 <= var5 + 1) {
                                    var15 = var10.topBlock
                                    var16 = var10.fillerBlock
                                    if (var12) {
                                        var15 = Blocks.AIR
                                    }
                                    if (var12) {
                                        var16 = Blocks.GRAVEL
                                    }
                                    if (var11) {
                                        var15 = Blocks.SAND
                                    }
                                    if (var11) {
                                        var16 = Blocks.SAND
                                    }
                                }
                                if (var17 < var5 && var15 == Blocks.AIR) {
//                                    var15 = Blocks.FLOWING_WATER
                                    var15 = Blocks.WATER
                                }
                                var14 = var13
                                if (var17 >= var5 - 1) {
                                    var3[var18] = var15
                                } else {
                                    var3[var18] = var16
                                }
                            } else if (var14 > 0) {
                                --var14
                                var3[var18] = var16
                            }
                        }
                    }
                }
            }
        }
    }
}
