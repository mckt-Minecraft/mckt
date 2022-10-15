package io.github.gaming32.mckt.world.gen.classic

import io.github.gaming32.mckt.floorToInt
import io.github.gaming32.mckt.objects.BlockState
import io.github.gaming32.mckt.world.Blocks
import kotlin.math.cos
import kotlin.math.sin


class MapGenCaves : MapGenBase() {
    override fun func666(var1: GenInfo, var2: Int, var3: Int, var4: Int, var5: Int, var6: Array<BlockState>) {
        var var7: Int = this.field946.nextInt(this.field946.nextInt(this.field946.nextInt(40) + 1) + 1)
        if (this.field946.nextInt(15) != 0) {
            var7 = 0
        }
        for (var8 in 0 until var7) {
            val var9 = (var2 * 16 + this.field946.nextInt(16)).toDouble()
            val var11 = this.field946.nextInt(this.field946.nextInt(120) + 8).toDouble()
            val var13 = (var3 * 16 + this.field946.nextInt(16)).toDouble()
            var var15 = 1
            if (this.field946.nextInt(4) == 0) {
                this.func669(var4, var5, var6, var9, var11, var13)
                var15 += this.field946.nextInt(4)
            }
            for (var16 in 0 until var15) {
                val var17: Float = this.field946.nextFloat() * Math.PI.toFloat() * 2.0f
                val var18: Float = (this.field946.nextFloat() - 0.5f) * 2.0f / 8.0f
                val var19: Float = this.field946.nextFloat() * 2.0f + this.field946.nextFloat()
                this.func668(var4, var5, var6, var9, var11, var13, var19, var17, var18, 0, 0, 1.0)
            }
        }
    }

    private fun func669(var1: Int, var2: Int, var3: Array<BlockState>, var4: Double, var6: Double, var8: Double) {
        this.func668(
            var1,
            var2,
            var3,
            var4,
            var6,
            var8,
            1.0f + this.field946.nextFloat() * 6.0f,
            0.0f,
            0.0f,
            -1,
            -1,
            0.5
        )
    }

    private fun func668(
        var1: Int,
        var2: Int,
        var3: Array<BlockState>,
        var4: Double,
        var6: Double,
        var8: Double,
        var10: Float,
        var11: Float,
        var12: Float,
        var13: Int,
        var14: Int,
        var15: Double
    ) {
        var var4tmp = var4
        var var6tmp = var6
        var var8tmp = var8
        var var11tmp = var11
        var var12tmp = var12
        var var13tmp = var13
        var var14tmp = var14
        val var17 = (var1 * 16 + 8).toDouble()
        val var19 = (var2 * 16 + 8).toDouble()
        var var21 = 0.0f
        var var22 = 0.0f
        val var23 = JuRandom(this.field946.nextLong())
        if (var14tmp <= 0) {
            val var24: Int = this.field947 * 16 - 16
            var14tmp = var24 - var23.nextInt(var24 / 4)
        }
        var var55 = false
        if (var13tmp == -1) {
            var13tmp = var14tmp / 2
            var55 = true
        }
        val var25: Int = var23.nextInt(var14tmp / 2) + var14tmp / 4
        val var26 = var23.nextInt(6) == 0
        while (var13tmp < var14tmp) {
            val var27 =
                1.5 + (sin(var13tmp.toFloat() * Math.PI.toFloat() / var14tmp.toFloat()) * var10 * 1.0f).toDouble()
            val var29 = var27 * var15
            val var31: Float = cos(var12tmp)
            val var32: Float = sin(var12tmp)
            var4tmp += (cos(var11tmp) * var31).toDouble()
            var6tmp += var32.toDouble()
            var8tmp += (sin(var11tmp) * var31).toDouble()
            var12tmp *= if (var26) {
                0.92f
            } else {
                0.7f
            }
            var12tmp += var22 * 0.1f
            var11tmp += var21 * 0.1f
            var22 *= 0.9f
            var21 *= 0.75f
            var22 += (var23.nextFloat() - var23.nextFloat()) * var23.nextFloat() * 2.0f
            var21 += (var23.nextFloat() - var23.nextFloat()) * var23.nextFloat() * 4.0f
            if (!var55 && var13tmp == var25 && var10 > 1.0f) {
                func668(
                    var1,
                    var2,
                    var3,
                    var4tmp,
                    var6tmp,
                    var8tmp,
                    var23.nextFloat() * 0.5f + 0.5f,
                    var11tmp - (Math.PI / 2).toFloat(),
                    var12tmp / 3.0f,
                    var13tmp,
                    var14tmp,
                    1.0
                )
                func668(
                    var1,
                    var2,
                    var3,
                    var4tmp,
                    var6tmp,
                    var8tmp,
                    var23.nextFloat() * 0.5f + 0.5f,
                    var11tmp + (Math.PI / 2).toFloat(),
                    var12tmp / 3.0f,
                    var13tmp,
                    var14tmp,
                    1.0
                )
                return
            }
            if (var55 || var23.nextInt(4) != 0) {
                val var33 = var4tmp - var17
                val var35 = var8tmp - var19
                val var37 = (var14tmp - var13tmp).toDouble()
                val var39 = (var10 + 2.0f + 16.0f).toDouble()
                if (var33 * var33 + var35 * var35 - var37 * var37 > var39 * var39) {
                    return
                }
                if (var4tmp >= var17 - 16.0 - var27 * 2.0
                    && var8tmp >= var19 - 16.0 - var27 * 2.0
                    && var4tmp <= var17 + 16.0 + var27 * 2.0
                    && var8tmp <= var19 + 16.0 + var27 * 2.0
                ) {
                    var var56: Int = (var4tmp - var27).floorToInt() - var1 * 16 - 1
                    var var34: Int = (var4tmp + var27).floorToInt() - var1 * 16 + 1
                    var var57: Int = (var6tmp - var29).floorToInt() - 1
                    var var36: Int = (var6tmp + var29).floorToInt() + 1
                    var var58: Int = (var8tmp - var27).floorToInt() - var2 * 16 - 1
                    var var38: Int = (var8tmp + var27).floorToInt() - var2 * 16 + 1
                    if (var56 < 0) {
                        var56 = 0
                    }
                    if (var34 > 16) {
                        var34 = 16
                    }
                    if (var57 < 1) {
                        var57 = 1
                    }
                    if (var36 > 120) {
                        var36 = 120
                    }
                    if (var58 < 0) {
                        var58 = 0
                    }
                    if (var38 > 16) {
                        var38 = 16
                    }
                    var var59 = false
                    var var40 = var56
                    while (!var59 && var40 < var34) {
                        var var41 = var58
                        while (!var59 && var41 < var38) {
                            var var42 = var36 + 1
                            while (!var59 && var42 >= var57 - 1) {
                                val var43 = (var40 * 16 + var41) * 128 + var42
//                                if (var42 >= 0 && var42 < 128) {
//                                    if (var3[var43] == Blocks.WATER || var3[var43] == Blocks.FLOWING_WATER) {
                                    if (var3[var43] == Blocks.WATER || var3[var43] == Blocks.WATER) {
                                        var59 = true
                                    }
                                    if (var42 != var57 - 1 && var40 != var56 && var40 != var34 - 1 && var41 != var58 && var41 != var38 - 1) {
                                        var42 = var57
                                    }
//                                }
                                --var42
                            }
                            ++var41
                        }
                        ++var40
                    }
                    if (!var59) {
                        for (var60 in var56 until var34) {
                            val var61 = ((var60 + var1 * 16).toDouble() + 0.5 - var4tmp) / var27
                            for (var62 in var58 until var38) {
                                val var44 = ((var62 + var2 * 16).toDouble() + 0.5 - var8tmp) / var27
                                var var46 = (var60 * 16 + var62) * 128 + var36
                                var var47 = false
                                for (var48 in var36 - 1 downTo var57) {
                                    val var49 = (var48.toDouble() + 0.5 - var6tmp) / var29
                                    if (var49 > -0.7 && var61 * var61 + var49 * var49 + var44 * var44 < 1.0) {
                                        val var51 = var3[var46]
                                        if (var51 == Blocks.GRASS_BLOCK) {
                                            var47 = true
                                        }
                                        if (var51 == Blocks.STONE || var51 == Blocks.DIRT || var51 == Blocks.GRASS_BLOCK) {
                                            if (var48 < 10) {
                                                var3[var46] = Blocks.LAVA
                                            } else {
                                                var3[var46] = Blocks.AIR
                                                if (var47 && var3[var46 - 1] == Blocks.DIRT) {
                                                    var3[var46 - 1] = Blocks.GRASS_BLOCK
                                                }
                                            }
                                        }
                                    }
                                    --var46
                                }
                            }
                        }
                        if (var55) {
                            break
                        }
                    }
                }
            }
            ++var13tmp
        }
    }
}
