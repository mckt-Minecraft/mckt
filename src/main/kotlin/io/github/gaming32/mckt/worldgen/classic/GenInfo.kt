package io.github.gaming32.mckt.worldgen.classic

class GenInfo(val seed: Long) {
    private val field4255 = NoiseGeneratorOctaves2(JuRandom(seed * 9871L), 4)
    private val field4254 = NoiseGeneratorOctaves2(JuRandom(seed * 39811L), 4)
    private val field4253 = NoiseGeneratorOctaves2(JuRandom(seed * 543321L), 2)
    val temperature = DoubleArray(256)
    val humidity = DoubleArray(256)
    private val field4257 = DoubleArray(256)

    fun loadBlockGeneratorData(var1: Array<MobSpawnerBase>, var2: Int, var3: Int, var4: Int, var5: Int) {
        require(var4 * var5 <= var1.size)
        field4255.func4100(temperature, var2.toDouble(), var3.toDouble(), var4, var4, 0.025, 0.025, 0.25)
        field4254.func4100(humidity, var2.toDouble(), var3.toDouble(), var4, var4, 0.05, 0.05, 1 / 3.0)
        field4253.func4100(field4257, var2.toDouble(), var3.toDouble(), var4, var4, 0.25, 0.25, 10 / 17.0)

        var var6 = 0
        for (var7 in 0 until var4) {
            for (var8 in 0 until var5) {
                val var9: Double = this.field4257[var6] * 1.1 + 0.5
                var var11 = 0.01
                var var13 = 1.0 - var11
                var var15 = (temperature[var6] * 0.15 + 0.7) * var13 + var9 * var11
                var11 = 0.002
                var13 = 1.0 - var11
                var var17 = (humidity[var6] * 0.15 + 0.5) * var13 + var9 * var11
                var15 = 1.0 - (1.0 - var15) * (1.0 - var15)
                if (var15 < 0.0) {
                    var15 = 0.0
                }
                if (var17 < 0.0) {
                    var17 = 0.0
                }
                if (var15 > 1.0) {
                    var15 = 1.0
                }
                if (var17 > 1.0) {
                    var17 = 1.0
                }
                temperature[var6] = var15
                humidity[var6] = var17
                var1[var6++] = MobSpawnerBase.getBiomeFromLookup(var15, var17)
            }
        }
    }
}
