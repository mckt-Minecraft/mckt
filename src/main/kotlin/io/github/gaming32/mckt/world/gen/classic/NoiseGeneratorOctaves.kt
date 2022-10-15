package io.github.gaming32.mckt.world.gen.classic

class NoiseGeneratorOctaves(var1: JuRandom, private val field938: Int) : NoiseGenerator() {
    private val field939 = Array(field938) { NoiseGeneratorPerlin(var1) }

    fun func648(
        var1: DoubleArray,
        var2: Double,
        var4: Double,
        var6: Double,
        var8: Int,
        var9: Int,
        var10: Int,
        var11: Double,
        var13: Double,
        var15: Double
    ) {
        require(var8 * var9 * var10 <= var1.size)
        var1.fill(0.0)
        var var20 = 1.0
        for (var19 in 0 until this.field938) {
            this.field939[var19].func646(
                var1,
                var2,
                var4,
                var6,
                var8,
                var9,
                var10,
                var11 * var20,
                var13 * var20,
                var15 * var20,
                var20
            )
            var20 /= 2.0
        }
    }

    fun func4103(
        var1: DoubleArray,
        var2: Int,
        var3: Int,
        var4: Int,
        var5: Int,
        var6: Double,
        var8: Double
    ) = this.func648(var1, var2.toDouble(), 10.0, var3.toDouble(), var4, 1, var5, var6, 1.0, var8)
}
