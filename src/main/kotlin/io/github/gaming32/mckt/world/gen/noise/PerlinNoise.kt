package io.github.gaming32.mckt.world.gen.noise

import kotlin.math.floor
import kotlin.random.Random

class PerlinNoise(seed: Long?) {
    companion object {
        private val PERM = intArrayOf(
            151, 160, 137, 91, 90, 15,
            131, 13, 201, 95, 96, 53, 194, 233, 7, 225, 140, 36, 103, 30, 69, 142, 8, 99, 37, 240, 21, 10, 23,
            190,  6, 148, 247, 120, 234, 75, 0, 26, 197, 62, 94, 252, 219, 203, 117, 35, 11, 32, 57, 177, 33,
            88, 237, 149, 56, 87, 174, 20, 125, 136, 171, 168,  68, 175, 74, 165, 71, 134, 139, 48, 27, 166,
            77, 146, 158, 231, 83, 111, 229, 122, 60, 211, 133, 230, 220, 105, 92, 41, 55, 46, 245, 40, 244,
            102, 143, 54,  65, 25, 63, 161,  1, 216, 80, 73, 209, 76, 132, 187, 208,  89, 18, 169, 200, 196,
            135, 130, 116, 188, 159, 86, 164, 100, 109, 198, 173, 186,  3, 64, 52, 217, 226, 250, 124, 123,
            5, 202, 38, 147, 118, 126, 255, 82, 85, 212, 207, 206, 59, 227, 47, 16, 58, 17, 182, 189, 28, 42,
            223, 183, 170, 213, 119, 248, 152,  2, 44, 154, 163,  70, 221, 153, 101, 155, 167,  43, 172, 9,
            129, 22, 39, 253,  19, 98, 108, 110, 79, 113, 224, 232, 178, 185,  112, 104, 218, 246, 97, 228,
            251, 34, 242, 193, 238, 210, 144, 12, 191, 179, 162, 241,  81, 51, 145, 235, 249, 14, 239, 107,
            49, 192, 214,  31, 181, 199, 106, 157, 184,  84, 204, 176, 115, 121, 50, 45, 127,  4, 150, 254,
            138, 236, 205, 93, 222, 114, 67, 29, 24, 72, 243, 141, 128, 195, 78, 66, 215, 61, 156, 180
        )
    }

    private val perm = (if (seed == null) PERM else PERM.copyOf()
        .apply { shuffle(Random(seed)) })
        .copyOf(257).also { it[256] = it[0] }

    fun noise1d(x: Double): Double {
        val x2 = floor(x).toInt() and 0xff
        val x3 = x - floor(x)
        val u = fade(x3)
        return lerp(u, grad1d(perm[x2], x3), grad1d(perm[x2 + 1], x3 - 1)) * 2
    }

    fun noise2d(x: Double, y: Double): Double {
        val x2 = floor(x).toInt() and 0xff
        val y2 = floor(y).toInt() and 0xff
        val x3 = x - floor(x)
        val y3 = y - floor(y)
        val u = fade(x3)
        val v = fade(y3)
        val a = (perm[x2] + y2) and 0xff
        val b = (perm[x2 + 1] + y2) and 0xff
        return lerp(v, lerp(u, grad2d(perm[a], x3, y3), grad2d(perm[b], x3 - 1, y3)),
                       lerp(u, grad2d(perm[a + 1], x3, y3 - 1), grad2d(perm[b + 1], x3 - 1, y3 - 1)))
    }

    fun noise3d(x: Double, y: Double, z: Double): Double {
        val x2 = floor(x).toInt() and 0xff
        val y2 = floor(y).toInt() and 0xff
        val z2 = floor(z).toInt() and 0xff
        val x3 = x - floor(x)
        val y3 = y - floor(y)
        val z3 = z - floor(z)
        val u = fade(x3)
        val v = fade(y3)
        val w = fade(z3)
        val a = (perm[x2] + y2) and 0xff
        val b = (perm[x2 + 1] + y2) and 0xff
        val aa = (perm[a] + z2) and 0xff
        val ba = (perm[b] + z2) and 0xff
        val ab = (perm[a + 1] + z2) and 0xff
        val bb = (perm[b + 1] + z2) and 0xff
        return lerp(w, lerp(v, lerp(u, grad3d(perm[aa], x3, y3, z3), grad3d(perm[ba], x3 - 1, y3, z3)),
                               lerp(u, grad3d(perm[ab], x3, y3 - 1, z3), grad3d(perm[bb], x3 - 1, y3 - 1, z3))),
                       lerp(v, lerp(u, grad3d(perm[aa + 1], x3, y3, z3 - 1), grad3d(perm[ba + 1], x3 - 1, y3, z3 - 1)),
                               lerp(u,
                                   grad3d(perm[ab + 1], x3, y3 - 1, z3 - 1),
                                   grad3d(perm[bb + 1], x3 - 1, y3 - 1, z3 - 1)
                               ))
        )
//        return lerp(w, lerp(v, lerp(u, grad3d(perm[aa  ], x3, y3  , z3  ), grad3d(perm[ba  ], x3-1, y3  , z3  )),
//            lerp(u, grad3d(perm[ab  ], x3, y3-1, z3  ), grad3d(perm[bb  ], x3-1, y3-1, z3  ))),
//            lerp(v, lerp(u, grad3d(perm[aa+1], x3, y3  , z3-1), grad3d(perm[ba+1], x3-1, y3  , z3-1)),
//                lerp(u, grad3d(perm[ab+1], x3, y3-1, z3-1), grad3d(perm[bb+1], x3-1, y3-1, z3-1))));
    }

    fun fbm1d(x: Double, octave: Int) = fbm(octave, x) { noise1d(it[0]) }

    fun fbm2d(x: Double, y: Double, octave: Int) = fbm(octave, x, y) { noise2d(it[0], it[1]) }

    fun fbm3d(x: Double, y: Double, z: Double, octave: Int) = fbm(octave, x, y, z) { noise3d(it[0], it[1], it[2]) }

    private inline fun fbm(octave: Int, vararg coords: Double, noise: (x: DoubleArray) -> Double): Double {
        var f = 0.0
        var w = 0.5
        repeat(octave) {
            f += w * noise(coords)
            repeat(coords.size) { i ->
                coords[i] *= 2.0
            }
            w *= 0.5
        }
        return f
    }

    private fun fade(t: Double) = t * t * t * (t * (t * 6 - 15) + 10)

    private fun lerp(t: Double, a: Double, b: Double): Double = a + t * (b - a)

    private fun grad1d(hash: Int, x: Double) = if ((hash and 1) != 0) x else -x

    private fun grad2d(hash: Int, x: Double, y: Double) =
        (if ((hash and 1) != 0) x else -x) + (if ((hash and 2) != 0) y else -y)

    private fun grad3d(hash: Int, x: Double, y: Double, z: Double): Double {
        val h = hash and 15
        val u = if (h < 8) x else y
        val v = if (h < 4) y else if (h == 12 || h == 14) x else z
        return (if ((h and 1) == 0) u else -u) + (if ((h and 2) == 0) v else -v)
    }
}
