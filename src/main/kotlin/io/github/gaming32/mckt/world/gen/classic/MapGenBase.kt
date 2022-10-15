package io.github.gaming32.mckt.world.gen.classic

import io.github.gaming32.mckt.objects.BlockState


open class MapGenBase {
    protected val field947 = 8
    protected val field946 = JuRandom()

    open fun func667(var1: ChunkProviderGenerate, var2: GenInfo, var3: Int, var4: Int, var5: Array<BlockState>) {
        val var6: Int = this.field947
        this.field946.setSeed(var2.seed)
        val var7: Long = this.field946.nextLong() / 2L * 2L + 1L
        val var9: Long = this.field946.nextLong() / 2L * 2L + 1L
        for (var11 in var3 - var6..var3 + var6) {
            for (var12 in var4 - var6..var4 + var6) {
                this.field946.setSeed(var11.toLong() * var7 + var12.toLong() * var9 xor var2.seed)
                this.func666(var2, var11, var12, var3, var4, var5)
            }
        }
    }

    protected open fun func666(var1: GenInfo, var2: Int, var3: Int, var4: Int, var5: Int, var6: Array<BlockState>) = Unit
}
