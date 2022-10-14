package io.github.gaming32.mckt.worldgen.classic

import kotlin.math.sqrt

class NoiseGenerator2(var1: JuRandom = JuRandom()) {
    companion object {
        private val field4317 = arrayOf(
            intArrayOf(1, 1, 0),
            intArrayOf(-1, 1, 0),
            intArrayOf(1, -1, 0),
            intArrayOf(-1, -1, 0),
            intArrayOf(1, 0, 1),
            intArrayOf(-1, 0, 1),
            intArrayOf(1, 0, -1),
            intArrayOf(-1, 0, -1),
            intArrayOf(0, 1, 1),
            intArrayOf(0, -1, 1),
            intArrayOf(0, 1, -1),
            intArrayOf(0, -1, -1)
        )
        private val field4315 = 0.5 * (sqrt(3.0) - 1.0)
        private val field4314 = (3.0 - sqrt(3.0)) / 6.0

        private fun func4113(var0: Double): Int {
            return if (var0 > 0.0) var0.toInt() else var0.toInt() - 1
        }

        private fun func4114(var0: IntArray, var1: Double, var3: Double): Double {
            return var0[0].toDouble() * var1 + var0[1].toDouble() * var3
        }
    }

    private val field4316 = IntArray(512)
    private val field4313 = var1.nextDouble() * 256.0
    private val field4312 = var1.nextDouble() * 256.0
//    val field4318 = var1.nextDouble() * 256.0

    init {
        repeat(256) { var2 ->
            field4316[var2] = var2
        }
        repeat(256) { var5 ->
            val var3 = var1.nextInt(256 - var5) + var5
            val var4 = field4316[var5]
            field4316[var5] = field4316[var3]
            field4316[var3] = var4
            field4316[var5 + 256] = field4316[var5]
        }
    }

    fun func4115(
        var1: DoubleArray,
        var2: Double,
        var4: Double,
        var6: Int,
        var7: Int,
        var8: Double,
        var10: Double,
        var12: Double
    ) {
        var var14 = 0
        for (var15 in 0 until var6) {
            val var16: Double = (var2 + var15.toDouble()) * var8 + this.field4313
            for (var18 in 0 until var7) {
                val var19: Double = (var4 + var18.toDouble()) * var10 + this.field4312
                val var27: Double = (var16 + var19) * field4315
                val var29: Int = func4113(var16 + var27)
                val var30: Int = func4113(var19 + var27)
                val var31: Double = (var29 + var30).toDouble() * field4314
                val var33 = var29.toDouble() - var31
                val var35 = var30.toDouble() - var31
                val var37 = var16 - var33
                val var39 = var19 - var35
                var var41: Byte
                var var42: Byte
                if (var37 > var39) {
                    var41 = 1
                    var42 = 0
                } else {
                    var41 = 0
                    var42 = 1
                }
                val var43: Double = var37 - var41.toDouble() + field4314
                val var45: Double = var39 - var42.toDouble() + field4314
                val var47: Double = var37 - 1.0 + 2.0 * field4314
                val var49: Double = var39 - 1.0 + 2.0 * field4314
                val var51 = var29 and 0xFF
                val var52 = var30 and 0xFF
                val var53: Int = this.field4316[var51 + this.field4316[var52]] % 12
                val var54: Int = this.field4316[var51 + var41 + this.field4316[var52 + var42]] % 12
                val var55: Int = this.field4316[var51 + 1 + this.field4316[var52 + 1]] % 12
                var var56 = 0.5 - var37 * var37 - var39 * var39
                var var21: Double
                if (var56 < 0.0) {
                    var21 = 0.0
                } else {
                    var56 *= var56
                    var21 = var56 * var56 * func4114(field4317[var53], var37, var39)
                }
                var var58 = 0.5 - var43 * var43 - var45 * var45
                var var23: Double
                if (var58 < 0.0) {
                    var23 = 0.0
                } else {
                    var58 *= var58
                    var23 = var58 * var58 * func4114(field4317[var54], var43, var45)
                }
                var var60 = 0.5 - var47 * var47 - var49 * var49
                var var25: Double
                if (var60 < 0.0) {
                    var25 = 0.0
                } else {
                    var60 *= var60
                    var25 = var60 * var60 * func4114(field4317[var55], var47, var49)
                }
                val var10001 = var14++
                var1[var10001] += 70.0 * (var21 + var23 + var25) * var12
            }
        }
    }
}
