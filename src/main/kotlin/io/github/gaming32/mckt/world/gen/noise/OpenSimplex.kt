package io.github.gaming32.mckt.world.gen.noise

import kotlin.math.floor

class OpenSimplex(seed: Long) {
    companion object {
        private val GRADIENTS3 = longArrayOf(
            -11, 4, 4, -4, 11, 4, -4, 4, 11,
            11, 4, 4, 4, 11, 4, 4, 4, 11,
            -11, -4, 4, -4, -11, 4, -4, -4, 11,
            11, -4, 4, 4, -11, 4, 4, -4, 11,
            -11, 4, -4, -4, 11, -4, -4, 4, -11,
            11, 4, -4, 4, 11, -4, 4, 4, -11,
            -11, -4, -4, -4, -11, -4, -4, -4, -11,
            11, -4, -4, 4, -11, -4, 4, -4, -11,
        )

        private const val STRETCH_CONSTANT3 = -1.0 / 6
        private const val SQUISH_CONSTANT3 = 0.366025403784439

        private const val NORM_CONSTANT3 = 103.0
    }

    private val perm = LongArray(256)
    private val permGradIndex3 = IntArray(256)

    init {
        val source = LongArray(256) { it.toLong() }
        var mathSeed = seed * 6364136223846793005 + 1442695040888963407
        mathSeed = mathSeed * 6364136223846793005 + 1442695040888963407
        mathSeed = mathSeed * 6364136223846793005 + 1442695040888963407
        for (i in 255 downTo 0) {
            mathSeed = mathSeed * 6364136223846793005 + 1442695040888963407
            var r = ((mathSeed + 31) % (i + 1)).toInt()
            if (r < 0) {
                r += i + 1
            }
            perm[i] = source[r]
            permGradIndex3[i] = ((perm[i] % (GRADIENTS3.size / 3)) * 3).toInt()
            source[r] = source[i]
        }
    }

    fun noise3(x: Double, y: Double, z: Double): Double {
        val stretchOffset = (x + y + z) * STRETCH_CONSTANT3
        val xs = x + stretchOffset
        val ys = y + stretchOffset
        val zs = z + stretchOffset

        val xsb = floor(xs).toLong()
        val ysb = floor(ys).toLong()
        val zsb = floor(zs).toLong()

        val squishOffset = (xsb + ysb + zsb) * SQUISH_CONSTANT3
        val xb = xsb + squishOffset
        val yb = ysb + squishOffset
        val zb = zsb + squishOffset

        val xins = xs - xsb
        val yins = ys - ysb
        val zins = zs - zsb

        val inSum = xins + yins + zins

        var dx0 = x - xb
        var dy0 = y - yb
        var dz0 = z - zb

        var value = 0.0
        val xsvExt0: Long
        var xsvExt1: Long
        val dxExt0: Double
        var dxExt1: Double
        var ysvExt0: Long
        var ysvExt1: Long
        var dyExt0: Double
        var dyExt1: Double
        val zsvExt0: Long
        var zsvExt1: Long
        val dzExt0: Double
        var dzExt1: Double
        if (inSum <= 1) {
            var aPoint = 0x01
            var aScore = xins
            var bPoint = 0x02
            var bScore = yins
            if (aScore >= bScore && zins > bScore) {
                bScore = zins
                bPoint = 0x04
            } else if (aScore < bScore && zins > aScore) {
                aScore = zins
                aPoint = 0x04
            }

            val wins = 1 - inSum
            if (wins > aScore || wins > bScore) {
                val c = if (bScore > aScore) bPoint else aPoint

                if ((c and 0x01) == 0) {
                    xsvExt0 = xsb - 1
                    xsvExt1 = xsb
                    dxExt0 = dx0 + 1
                    dxExt1 = dx0
                } else {
                    xsvExt0 = xsb + 1
                    xsvExt1 = xsvExt0
                    dxExt0 = dx0 - 1
                    dxExt1 = dxExt0
                }

                if ((c and 0x02) == 0) {
                    ysvExt0 = ysb
                    ysvExt1 = ysvExt0
                    dyExt0 = dy0
                    dyExt1 = dyExt0
                    if ((c and 0x01) == 0) {
                        ysvExt1--
                        dyExt1++
                    } else {
                        ysvExt0--
                        dyExt0++
                    }
                } else {
                    ysvExt0 = ysb + 1
                    ysvExt1 = ysvExt0
                    dyExt0 = dy0 - 1
                    dyExt1 = dyExt0
                }

                if ((c and 0x04) == 0) {
                    zsvExt0 = zsb
                    zsvExt1 = zsb - 1
                    dzExt0 = dz0
                    dzExt1 = dz0 + 1
                } else {
                    zsvExt0 = zsb + 1
                    zsvExt1 = zsvExt0
                    dzExt0 = dz0 - 1
                    dzExt1 = dzExt0
                }
            } else {
                val c = aPoint or bPoint

                if ((c and 0x01) == 0) {
                    xsvExt0 = xsb
                    xsvExt1 = xsb - 1
                    dxExt0 = dx0 - 2 * SQUISH_CONSTANT3
                    dxExt1 = dx0 + 1 - SQUISH_CONSTANT3
                } else {
                    xsvExt0 = xsb + 1
                    xsvExt1 = xsvExt0
                    dxExt0 = dx0 - 1 - 2 * SQUISH_CONSTANT3
                    dxExt1 = dx0 - 1 - SQUISH_CONSTANT3
                }

                if ((c and 0x02) == 0) {
                    ysvExt0 = ysb
                    ysvExt1 = ysb - 1
                    dyExt0 = dy0 - 2 * SQUISH_CONSTANT3
                    dyExt1 = dy0 + 1 - SQUISH_CONSTANT3
                } else {
                    ysvExt0 = ysb + 1
                    ysvExt1 = ysvExt0
                    dyExt0 = dy0 - 1 - 2 * SQUISH_CONSTANT3
                    dyExt1 = dy0 - 1 - SQUISH_CONSTANT3
                }

                if ((c and 0x04) == 0) {
                    zsvExt0 = zsb
                    zsvExt1 = zsb - 1
                    dzExt0 = dz0 - 2 * SQUISH_CONSTANT3
                    dzExt1 = dz0 + 1 - SQUISH_CONSTANT3
                } else {
                    zsvExt0 = zsb + 1
                    zsvExt1 = zsvExt0
                    dzExt0 = dz0 - 1 - 2 * SQUISH_CONSTANT3
                    dzExt1 = dz0 - 1 - SQUISH_CONSTANT3
                }
            }

            var attn0 = 2 - dx0 * dx0 - dy0 * dy0 - dz0 * dz0
            if (attn0 > 0) {
                attn0 *= attn0
                value += attn0 * attn0 * extrapolate3(xsb + 0, ysb + 0, zsb + 0, dx0, dy0, dz0)
            }

            val dx1 = dx0 - 1 - SQUISH_CONSTANT3
            val dy1 = dy0 - 0 - SQUISH_CONSTANT3
            val dz1 = dz0 - 0 - SQUISH_CONSTANT3
            var attn1 = 2 - dx1 * dx1 - dy1 * dy1 - dz1 * dz1
            if (attn1 > 0) {
                attn1 *= attn1
                value += attn1 * attn1 * extrapolate3(xsb + 1, ysb + 0, zsb + 0, dx1, dy1, dz1)
            }

            val dx2 = dx0 - 0 - SQUISH_CONSTANT3
            val dy2 = dy0 - 1 - SQUISH_CONSTANT3
            var attn2 = 2 - dx2 * dx2 - dy2 * dy2 - dz1 * dz1
            if (attn2 > 0) {
                attn2 *= attn2
                value += attn2 * attn2 * extrapolate3(xsb + 0, ysb + 1, zsb + 0, dx2, dy2, dz1)
            }

            val dz3 = dz0 - 1 - SQUISH_CONSTANT3
            var attn3 = 2 - dx2 * dx2 - dy1 * dy1 - dz3 * dz3
            if (attn3 > 0) {
                attn3 *= attn3
                value += attn3 * attn3 * extrapolate3(xsb + 0, ysb + 0, zsb + 1, dx2, dy1, dz3)
            }
        } else if (inSum >= 2) {
            var aPoint = 0x06
            var aScore = xins
            var bPoint = 0x05
            var bScore = yins
            if (aScore <= bScore && zins < bScore) {
                bScore = zins
                bPoint = 0x03
            } else if (aScore > bScore && zins < aScore) {
                aScore = zins
                aPoint = 0x03
            }

            val wins = 3 - inSum
            if (wins < aScore || wins < bScore) {
                val c = if (bScore < aScore) bPoint else aPoint

                if ((c and 0x01) != 0) {
                    xsvExt0 = xsb + 2
                    xsvExt1 = xsb + 1
                    dxExt0 = dx0 - 2 - 3 * SQUISH_CONSTANT3
                    dxExt1 = dx0 - 1 - 3 * SQUISH_CONSTANT3
                } else {
                    xsvExt0 = xsb
                    xsvExt1 = xsvExt0
                    dxExt0 = dx0 - 3 * SQUISH_CONSTANT3
                    dxExt1 = dxExt0
                }

                if ((c and 0x02) != 0) {
                    ysvExt0 = ysb + 1
                    ysvExt1 = ysvExt0
                    dyExt0 = dy0 - 1 - 3 * SQUISH_CONSTANT3
                    dyExt1 = dyExt0
                    if ((c and 0x01) != 0) {
                        ysvExt1++
                        dyExt1--
                    } else {
                        ysvExt0++
                        dyExt0--
                    }
                } else {
                    ysvExt0 = ysb
                    ysvExt1 = ysvExt0
                    dyExt0 = dy0 - 3 * SQUISH_CONSTANT3
                    dyExt1 = dyExt0
                }

                if ((c and 0x04) != 0) {
                    zsvExt0 = zsb + 1
                    zsvExt1 = zsb + 2
                    dzExt0 = dz0 - 1 - 3 * SQUISH_CONSTANT3
                    dzExt1 = dz0 - 2 - 3 * SQUISH_CONSTANT3
                } else {
                    zsvExt0 = zsb
                    zsvExt1 = zsvExt0
                    dzExt0 = dz0 - 3 * SQUISH_CONSTANT3
                    dzExt1 = dzExt0
                }
            } else {
                val c = aPoint and bPoint

                if ((c and 0x01) != 0) {
                    xsvExt0 = xsb + 1
                    xsvExt1 = xsb + 2
                    dxExt0 = dx0 - 1 - SQUISH_CONSTANT3
                    dxExt1 = dx0 - 2 - 2 * SQUISH_CONSTANT3
                } else {
                    xsvExt0 = xsb
                    xsvExt1 = xsvExt0
                    dxExt0 = dx0 - SQUISH_CONSTANT3
                    dxExt1 = dx0 - 2 * SQUISH_CONSTANT3
                }

                if ((c and 0x02) != 0) {
                    ysvExt0 = ysb + 1
                    ysvExt1 = ysb + 2
                    dyExt0 = dy0 - 1 - SQUISH_CONSTANT3
                    dyExt1 = dy0 - 2 - 2 * SQUISH_CONSTANT3
                } else {
                    ysvExt0 = ysb
                    ysvExt1 = ysvExt0
                    dyExt0 = dy0 - SQUISH_CONSTANT3
                    dyExt1 = dy0 - 2 * SQUISH_CONSTANT3
                }

                if ((c and 0x04) != 0) {
                    zsvExt0 = zsb + 1
                    zsvExt1 = zsb + 2
                    dzExt0 = dz0 - 1 - SQUISH_CONSTANT3
                    dzExt1 = dz0 - 2 - 2 * SQUISH_CONSTANT3
                } else {
                    zsvExt0 = zsb
                    zsvExt1 = zsvExt0
                    dzExt0 = dz0 - SQUISH_CONSTANT3
                    dzExt1 = dz0 - 2 * SQUISH_CONSTANT3
                }
            }

            val dx3 = dx0 - 1 - 2 * SQUISH_CONSTANT3
            val dy3 = dy0 - 1 - 2 * SQUISH_CONSTANT3
            val dz3 = dz0 - 0 - 2 * SQUISH_CONSTANT3
            var attn3 = 2 - dx3 * dx3 - dy3 * dy3 - dz3 * dz3
            if (attn3 > 0) {
                attn3 *= attn3
                value += attn3 * attn3 * extrapolate3(xsb + 1, ysb + 1, zsb + 0, dx3, dy3, dz3)
            }

            val dy2 = dy0 - 0 - 2 * SQUISH_CONSTANT3
            val dz2 = dz0 - 1 - 2 * SQUISH_CONSTANT3
            var attn2 = 2 - dx3 * dx3 - dy2 * dy2 - dz2 * dz2
            if (attn2 > 0) {
                attn2 *= attn2
                value += attn2 * attn2 * extrapolate3(xsb + 1, ysb + 0, zsb + 1, dx3, dy2, dz2)
            }

            val dx1 = dx0 - 0 - 2 * SQUISH_CONSTANT3
            var attn1 = 2 - dx1 * dx1 - dy3 * dy3 - dz2 * dz2
            if (attn1 > 0) {
                attn1 *= attn1
                value += attn1 * attn1 * extrapolate3(xsb + 0, ysb + 1, zsb + 1, dx1, dy3, dz2)
            }

            dx0 = dx0 - 1 - 3 * SQUISH_CONSTANT3
            dy0 = dy0 - 1 - 3 * SQUISH_CONSTANT3
            dz0 = dz0 - 1 - 3 * SQUISH_CONSTANT3
            var attn0 = 2 - dx0 * dx0 - dy0 * dy0 - dz0 * dz0
            if (attn0 > 0) {
                attn0 *= attn0
                value += attn0 * attn0 * extrapolate3(xsb + 1, ysb + 1, zsb + 1, dx0, dy0, dz0)
            }
        } else {
            val p1 = xins + yins
            val aScore: Double
            var aPoint: Int
            var aIsFurtherSide: Boolean
            if (p1 > 0) {
                aScore = p1 - 1
                aPoint = 0x03
                aIsFurtherSide = true
            } else {
                aScore = 1 - p1
                aPoint = 0x04
                aIsFurtherSide = false
            }

            val p2 = xins + zins
            val bScore: Double
            var bPoint: Int
            var bIsFurtherSide: Boolean
            if (p2 > 1) {
                bScore = p2 - 1
                bPoint = 0x05
                bIsFurtherSide = true
            } else {
                bScore = 1 - p2
                bPoint = 0x02
                bIsFurtherSide = false
            }

            val p3 = yins + zins
            val score: Double
            if (p3 > 1) {
                score = p3 - 1
                if (aScore <= bScore && aScore < score) {
                    aPoint = 0x06
                    aIsFurtherSide = true
                } else if (aScore > bScore && bScore < score) {
                    bPoint = 0x06
                    bIsFurtherSide = true
                }
            } else {
                score = 1 - p3
                if (aScore <= bScore && aScore < score) {
                    aPoint = 0x01
                    aIsFurtherSide = false
                } else if (aScore > bScore && bScore < score) {
                    bPoint = 0x01
                    bIsFurtherSide = false
                }
            }

            if (aIsFurtherSide == bIsFurtherSide) {
                if (aIsFurtherSide) {
                    dxExt0 = dx0 - 1 - 3 * SQUISH_CONSTANT3
                    dyExt0 = dy0 - 1 - 3 * SQUISH_CONSTANT3
                    dzExt0 = dz0 - 1 - 3 * SQUISH_CONSTANT3
                    xsvExt0 = xsb + 1
                    ysvExt0 = ysb + 1
                    zsvExt0 = zsb + 1

                    val c = aPoint and bPoint
                    if ((c and 0x01) != 0) {
                        dxExt1 = dx0 - 2 - 2 * SQUISH_CONSTANT3
                        dyExt1 = dy0 - 2 * SQUISH_CONSTANT3
                        dzExt1 = dz0 - 2 * SQUISH_CONSTANT3
                        xsvExt1 = xsb + 2
                        ysvExt1 = ysb
                        zsvExt1 = zsb
                    } else if ((c and 0x02) != 0) {
                        dxExt1 = dx0 - 2 * SQUISH_CONSTANT3
                        dyExt1 = dy0 - 2 - 2 * SQUISH_CONSTANT3
                        dzExt1 = dz0 - 2 * SQUISH_CONSTANT3
                        xsvExt1 = xsb
                        ysvExt1 = ysb + 2
                        zsvExt1 = zsb
                    } else {
                        dxExt1 = dx0 - 2 * SQUISH_CONSTANT3
                        dyExt1 = dy0 - 2 * SQUISH_CONSTANT3
                        dzExt1 = dz0 - 2 - 2 * SQUISH_CONSTANT3
                        xsvExt1 = xsb
                        ysvExt1 = ysb
                        zsvExt1 = zsb + 2
                    }
                } else {
                    dxExt0 = dx0
                    dyExt0 = dy0
                    dzExt0 = dz0
                    xsvExt0 = xsb
                    ysvExt0 = ysb
                    zsvExt0 = zsb

                    val c = aPoint or bPoint
                    if ((c and 0x01) == 0) {
                        dxExt1 = dx0 + 1 - SQUISH_CONSTANT3
                        dyExt1 = dy0 - 1 - SQUISH_CONSTANT3
                        dzExt1 = dz0 - 1 - SQUISH_CONSTANT3
                        xsvExt1 = xsb - 1
                        ysvExt1 = ysb + 1
                        zsvExt1 = zsb + 1
                    } else if ((c and 0x02) == 0) {
                        dxExt1 = dx0 - 1 - SQUISH_CONSTANT3
                        dyExt1 = dy0 + 1 - SQUISH_CONSTANT3
                        dzExt1 = dz0 - 1 - SQUISH_CONSTANT3
                        xsvExt1 = xsb + 1
                        ysvExt1 = ysb - 1
                        zsvExt1 = zsb + 1
                    } else {
                        dxExt1 = dx0 - 1 - SQUISH_CONSTANT3
                        dyExt1 = dy0 - 1 - SQUISH_CONSTANT3
                        dzExt1 = dz0 + 1 - SQUISH_CONSTANT3
                        xsvExt1 = xsb + 1
                        ysvExt1 = ysb + 1
                        zsvExt1 = zsb - 1
                    }
                }
            } else {
                val c1: Int
                val c2: Int
                if (aIsFurtherSide) {
                    c1 = aPoint
                    c2 = bPoint
                } else {
                    c1 = bPoint
                    c2 = aPoint
                }

                if ((c1 and 0x01) == 0) {
                    dxExt0 = dx0 + 1 - SQUISH_CONSTANT3
                    dyExt0 = dy0 - 1 - SQUISH_CONSTANT3
                    dzExt0 = dz0 - 1 - SQUISH_CONSTANT3
                    xsvExt0 = xsb - 1
                    ysvExt0 = ysb + 1
                    zsvExt0 = zsb + 1
                } else if ((c1 and 0x02) == 0) {
                    dxExt0 = dx0 - 1 - SQUISH_CONSTANT3
                    dyExt0 = dy0 + 1 - SQUISH_CONSTANT3
                    dzExt0 = dz0 - 1 - SQUISH_CONSTANT3
                    xsvExt0 = xsb + 1
                    ysvExt0 = ysb - 1
                    zsvExt0 = zsb + 1
                } else {
                    dxExt0 = dx0 - 1 - SQUISH_CONSTANT3
                    dyExt0 = dy0 - 1 - SQUISH_CONSTANT3
                    dzExt0 = dz0 + 1 - SQUISH_CONSTANT3
                    xsvExt0 = xsb + 1
                    ysvExt0 = ysb + 1
                    zsvExt0 = zsb - 1
                }

                dxExt1 = dx0 - 2 * SQUISH_CONSTANT3
                dyExt1 = dy0 - 2 * SQUISH_CONSTANT3
                dzExt1 = dz0 - 2 * SQUISH_CONSTANT3
                xsvExt1 = xsb
                ysvExt1 = ysb
                zsvExt1 = zsb
                if ((c2 and 0x01) != 0) {
                    dxExt1 -= 2
                    xsvExt1 += 2
                } else if ((c2 and 0x02) != 0) {
                    dyExt1 -= 2
                    ysvExt1 += 2
                } else {
                    dzExt1 -= 2
                    zsvExt1 += 2
                }
            }

            val dx1 = dx0 - 1 - SQUISH_CONSTANT3
            val dy1 = dy0 - 0 - SQUISH_CONSTANT3
            val dz1 = dz0 - 0 - SQUISH_CONSTANT3
            var attn1 = 2 - dx1 * dx1 - dy1 * dy1 - dz1 * dz1
            if (attn1 > 0) {
                attn1 *= attn1
                value += attn1 * attn1 * extrapolate3(xsb + 1, ysb + 0, zsb + 0, dx1, dy1, dz1)
            }

            val dx2 = dx0 - 0 - SQUISH_CONSTANT3
            val dy2 = dy0 - 1 - SQUISH_CONSTANT3
            var attn2 = 2 - dx2 * dx2 - dy2 * dy2 - dz1 * dz1
            if (attn2 > 0) {
                attn2 *= attn2
                value += attn2 * attn2 * extrapolate3(xsb + 0, ysb + 1, zsb + 0, dx2, dy2, dz1)
            }

            val dz3 = dz0 - 1 - SQUISH_CONSTANT3
            var attn3 = 2 - dx2 * dx2 - dy1 * dy1 - dz3 * dz3
            if (attn3 > 0) {
                attn3 *= attn3
                value += attn3 * attn3 * extrapolate3(xsb + 0, ysb + 0, zsb + 1, dx2, dy1, dz3)
            }

            val dx4 = dx0 - 1 - 2 * SQUISH_CONSTANT3
            val dy4 = dy0 - 1 - 2 * SQUISH_CONSTANT3
            val dz4 = dz0 - 0 - 2 * SQUISH_CONSTANT3
            var attn4 = 2 - dx4 * dx4 - dy4 * dy4 - dz4 * dz4
            if (attn4 > 0) {
                attn4 *= attn4
                value += attn4 * attn4 * extrapolate3(xsb + 1, ysb + 1, zsb + 0, dx4, dy4, dz4)
            }

            val dy5 = dy0 - 0 - 2 * SQUISH_CONSTANT3
            val dz5 = dz0 - 1 - 2 * SQUISH_CONSTANT3
            var attn5 = 2 - dx4 * dx4 - dy5 * dy5 - dz5 * dz5
            if (attn5 > 0) {
                attn5 *= attn5
                value += attn5 * attn5 * extrapolate3(xsb + 1, ysb + 0, zsb + 1, dx4, dy5, dz5)
            }

            val dx6 = dx0 - 0 - 2 * SQUISH_CONSTANT3
            var attn6 = 2 - dx6 * dx6 - dy4 * dy4 - dz5 * dz5
            if (attn6 > 0) {
                attn6 *= attn6
                value += attn6 * attn6 * extrapolate3(xsb + 0, ysb + 1, zsb + 1, dx6, dy4, dz5)
            }
        }

        var attnExt0 = 2 - dxExt0 * dxExt0 - dyExt0 * dyExt0 - dzExt0 * dzExt0
        if (attnExt0 > 0) {
            attnExt0 *= attnExt0
            value += attnExt0 * attnExt0 *
                extrapolate3(xsvExt0, ysvExt0, zsvExt0, dxExt0, dyExt0, dzExt0)
        }

        var attnExt1 = 2 - dxExt1 * dxExt1 - dyExt1 * dyExt1 - dzExt1 * dzExt1
        if (attnExt1 > 0) {
            attnExt1 *= attnExt1
            value += attnExt1 * attnExt1 *
                extrapolate3(xsvExt1, ysvExt1, zsvExt1, dxExt1, dyExt1, dzExt1)
        }

        return value / NORM_CONSTANT3
    }

    private fun extrapolate3(xsb: Long, ysb: Long, zsb: Long, dx: Double, dy: Double, dz: Double): Double {
        val index = permGradIndex3[
            ((perm[
                ((perm[
                    (xsb and 0xFF).toInt()
                ] + ysb) and 0xFF).toInt()
            ] + zsb) and 0xFF).toInt()
        ]
        val g1 = GRADIENTS3[index]
        val g2 = GRADIENTS3[index + 1]
        val g3 = GRADIENTS3[index + 2]
        return g1 * dx + g2 * dy + g3 * dz
    }
}
