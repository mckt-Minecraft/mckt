package io.github.gaming32.mckt.util

import io.github.gaming32.mckt.squared
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.random.Random

class GaussianGenerator(private val base: Random) {
    companion object {
        val DEFAULT = GaussianGenerator(Random.Default)

        fun reset() = DEFAULT.reset()
        fun next() = DEFAULT.next()
    }

    private var nextNextGaussian: Double? = null

    fun reset() {
        nextNextGaussian = null
    }

    fun next(): Double {
        nextNextGaussian?.let {
            nextNextGaussian = null
            return it
        }
        var d: Double
        var e: Double
        var f: Double
        do {
            d = 2.0 * base.nextDouble() - 1.0
            e = 2.0 * base.nextDouble() - 1.0
            f = d.squared() * e.squared()
        } while (f >= 1.0 || f == 0.0)

        val g = sqrt(-2.0 * ln(f) / f)
        nextNextGaussian = e * g
        return d * g
    }
}
