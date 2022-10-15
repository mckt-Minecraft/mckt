package io.github.gaming32.mckt.world.gen.classic

class NoiseGeneratorOctaves2(var1: JuRandom, private val field4307: Int) : NoiseGenerator() {
    private val field4308 = Array(field4307) { NoiseGenerator2(var1) }

    fun func4100(
        var1: DoubleArray,
        var2: Double,
        var4: Double,
        var6: Int,
        var7: Int,
        var8: Double,
        var10: Double,
        var12: Double,
        var14: Double = 0.5
    ) {
        val var8tmp = var8 / 1.5
        val var10tmp = var10 / 1.5
        require(var6 * var7 <= var1.size)
        var1.fill(0.0)

        var var23 = 1.0
        var var18 = 1.0

        repeat(field4307) { var20 ->
            field4308[var20].func4115(var1, var2, var4, var6, var7, var8tmp * var18, var10tmp * var18, 0.55 / var23)
            var18 *= var12
            var23 *= var14
        }
    }
}
