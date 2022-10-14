package io.github.gaming32.mckt.worldgen.classic

class NoiseGeneratorPerlin(var1: JuRandom = JuRandom()) : NoiseGenerator() {
    private val xCoord = var1.nextDouble() * 256.0
    private val yCoord = var1.nextDouble() * 256.0
    private val zCoord = var1.nextDouble() * 256.0
    private val permutations = IntArray(512)

    init {
        repeat(256) { var2 ->
            permutations[var2] = var2
        }
        repeat(256) { var5 ->
            val var3 = var1.nextInt(256 - var5) + var5
            val var4 = permutations[var5]
            permutations[var5] = permutations[var3]
            permutations[var3] = var4
            permutations[var5 + 256] = permutations[var5]
        }
    }

    fun func646(
        var1: DoubleArray,
        var2: Double,
        var4: Double,
        var6: Double,
        var8: Int,
        var9: Int,
        var10: Int,
        var11: Double,
        var13: Double,
        var15: Double,
        var17: Double
    ) {
        if (var9 == 1) {
            var var64: Int
            var var66: Int
            var var21: Int
            var var69: Int
            var var72: Double
            var var76: Double
            var var80 = 0
            val var82 = 1.0 / var17
            for (var30 in 0 until var8) {
                var var83 = (var2 + var30.toDouble()) * var11 + xCoord
                var var85 = var83.toInt()
                if (var83 < var85.toDouble()) {
                    --var85
                }
                val var34 = var85 and 0xFF
                var83 -= var85.toDouble()
                val var86 = var83 * var83 * var83 * (var83 * (var83 * 6.0 - 15.0) + 10.0)
                for (var87 in 0 until var10) {
                    var var89 = (var6 + var87.toDouble()) * var15 + zCoord
                    var var91 = var89.toInt()
                    if (var89 < var91.toDouble()) {
                        --var91
                    }
                    val var92 = var91 and 0xFF
                    var89 -= var91.toDouble()
                    val var93 = var89 * var89 * var89 * (var89 * (var89 * 6.0 - 15.0) + 10.0)
                    var64 = permutations[var34] + 0
                    var66 = permutations[var64] + var92
                    var21 = permutations[var34 + 1] + 0
                    var69 = permutations[var21] + var92
                    var72 = this.lerp(
                        var86,
                        this.func4102(permutations[var66], var83, var89),
                        this.grad(permutations[var69], var83 - 1.0, 0.0, var89)
                    )
                    var76 = this.lerp(
                        var86,
                        this.grad(permutations[var66 + 1], var83, 0.0, var89 - 1.0),
                        this.grad(permutations[var69 + 1], var83 - 1.0, 0.0, var89 - 1.0)
                    )
                    val var94: Double = this.lerp(var93, var72, var76)
                    val var97 = var80++
                    var1[var97] += var94 * var82
                }
            }
        } else {
            var var19 = 0
            val var20 = 1.0 / var17
            var var22 = -1
            var var23: Int
            var var24: Int
            var var25: Int
            var var26: Int
            var var27: Int
            var var28: Int
            var var29 = 0.0
            var var31 = 0.0
            var var33 = 0.0
            var var35 = 0.0
            for (var37 in 0 until var8) {
                var var38 = (var2 + var37.toDouble()) * var11 + xCoord
                var var40 = var38.toInt()
                if (var38 < var40.toDouble()) {
                    --var40
                }
                val var41 = var40 and 0xFF
                var38 -= var40.toDouble()
                val var42 = var38 * var38 * var38 * (var38 * (var38 * 6.0 - 15.0) + 10.0)
                for (var44 in 0 until var10) {
                    var var45 = (var6 + var44.toDouble()) * var15 + zCoord
                    var var47 = var45.toInt()
                    if (var45 < var47.toDouble()) {
                        --var47
                    }
                    val var48 = var47 and 0xFF
                    var45 -= var47.toDouble()
                    val var49 = var45 * var45 * var45 * (var45 * (var45 * 6.0 - 15.0) + 10.0)
                    for (var51 in 0 until var9) {
                        var var52 = (var4 + var51.toDouble()) * var13 + yCoord
                        var var54 = var52.toInt()
                        if (var52 < var54.toDouble()) {
                            --var54
                        }
                        val var55 = var54 and 0xFF
                        var52 -= var54.toDouble()
                        val var56 = var52 * var52 * var52 * (var52 * (var52 * 6.0 - 15.0) + 10.0)
                        if (var51 == 0 || var55 != var22) {
                            var22 = var55
                            var23 = permutations[var41] + var55
                            var24 = permutations[var23] + var48
                            var25 = permutations[var23 + 1] + var48
                            var26 = permutations[var41 + 1] + var55
                            var27 = permutations[var26] + var48
                            var28 = permutations[var26 + 1] + var48
                            var29 = this.lerp(
                                var42,
                                this.grad(permutations[var24], var38, var52, var45),
                                this.grad(permutations[var27], var38 - 1.0, var52, var45)
                            )
                            var31 = this.lerp(
                                var42,
                                this.grad(permutations[var25], var38, var52 - 1.0, var45),
                                this.grad(permutations[var28], var38 - 1.0, var52 - 1.0, var45)
                            )
                            var33 = this.lerp(
                                var42,
                                this.grad(permutations[var24 + 1], var38, var52, var45 - 1.0),
                                this.grad(permutations[var27 + 1], var38 - 1.0, var52, var45 - 1.0)
                            )
                            var35 = this.lerp(
                                var42,
                                this.grad(permutations[var25 + 1], var38, var52 - 1.0, var45 - 1.0),
                                this.grad(permutations[var28 + 1], var38 - 1.0, var52 - 1.0, var45 - 1.0)
                            )
                        }
                        val var58: Double = this.lerp(var56, var29, var31)
                        val var60: Double = this.lerp(var56, var33, var35)
                        val var62: Double = this.lerp(var49, var58, var60)
                        val var10001 = var19++
                        var1[var10001] += var62 * var20
                    }
                }
            }
        }
    }

    private fun grad(var1: Int, var2: Double, var4: Double, var6: Double): Double {
        val var8 = var1 and 15
        val var9 = if (var8 < 8) var2 else var4
        val var11 = if (var8 < 4) var4 else if (var8 != 12 && var8 != 14) var6 else var2
        return (if (var8 and 1 == 0) var9 else -var9) + if (var8 and 2 == 0) var11 else -var11
    }

    private fun lerp(var1: Double, var3: Double, var5: Double): Double {
        return var3 + var1 * (var5 - var3)
    }

    private fun func4102(var1: Int, var2: Double, var4: Double): Double {
        val var6 = var1 and 15
        val var7 = (1 - (var6 and 8 shr 3)).toDouble() * var2
        val var9 = if (var6 < 4) 0.0 else if (var6 != 12 && var6 != 14) var4 else var2
        return (if (var6 and 1 == 0) var7 else -var7) + if (var6 and 2 == 0) var9 else -var9
    }
}
