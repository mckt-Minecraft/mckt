package io.github.gaming32.mckt.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

// Gotten from Minecraft
@Serializable(SimpleBitStorage.SimpleBitStorageSerializer::class)
class SimpleBitStorage(val bits: Int, val size: Int, input: LongArray? = null) {
    val data: LongArray
    private val mask: Long
    private val valuesPerLong: Int
    private val divideMultiply: Int
    private val divideAdd: Int
    private val divideShift: Int

    init {
        require(bits in 1..32)
        mask = (1L shl bits) - 1L
        valuesPerLong = (64 / bits).toChar().code
        val magicIndex = 3 * (valuesPerLong - 1)
        divideMultiply = MAGIC[magicIndex + 0]
        divideAdd = MAGIC[magicIndex + 1]
        divideShift = MAGIC[magicIndex + 2]
        val dataSize = (size + valuesPerLong - 1) / valuesPerLong
        data = if (input != null) {
            if (input.size != dataSize) {
                throw InitializationException(
                    "Invalid length given for storage, got: ${input.size} but expected: $dataSize"
                )
            }
            input
        } else {
            LongArray(dataSize)
        }
    }

    constructor(bits: Int, size: Int, ints: IntArray) : this(bits, size) {
        var k = 0
        var l = 0
        while (l <= size - valuesPerLong) {
            var m = 0L
            for (n in valuesPerLong - 1 downTo 0) {
                m = m shl bits
                m = m or (ints[l + n].toLong() and mask)
            }
            data[k++] = m
            l += valuesPerLong
        }
        val o = size - l
        if (o > 0) {
            var p = 0L
            for (q in o - 1 downTo 0) {
                p = p shl bits
                p = p or (ints[l + q].toLong() and mask)
            }
            data[k] = p
        }
    }

    private fun cellIndex(i: Int): Int {
        val l = Integer.toUnsignedLong(divideMultiply)
        val m = Integer.toUnsignedLong(divideAdd)
        return (i.toLong() * l + m shr 32 shr divideShift).toInt()
    }

    fun getAndSet(i: Int, j: Int): Int {
        require(i in 0 until size) { "Index out of bounds" }
        require(j.toLong() in 0L..mask) { "Value out of range" }
        val k = cellIndex(i)
        val l = data[k]
        val m = (i - k * valuesPerLong) * bits
        val n = (l shr m and mask).toInt()
        data[k] = l and (mask shl m).inv() or (j.toLong() and mask shl m)
        return n
    }

    operator fun set(i: Int, j: Int) {
        require(i in 0 until size) { "Index out of bounds" }
        require(j.toLong() in 0..mask) { "Value out of range" }
        val k = cellIndex(i)
        val l = data[k]
        val m = (i - k * valuesPerLong) * bits
        data[k] = l and (mask shl m).inv() or (j.toLong() and mask shl m)
    }

    operator fun get(i: Int): Int {
        require(i in 0 until size) { "Index out of bounds" }
        val j = cellIndex(i)
        val l = data[j]
        val k = (i - j * valuesPerLong) * bits
        return (l shr k and mask).toInt()
    }

    fun getAll(intConsumer: (Int) -> Unit) {
        var i = 0
        for (l in data) {
            var l2 = l
            for (j in 0 until valuesPerLong) {
                intConsumer((l2 and mask).toInt())
                l2 = l shr bits
                if (++i >= size) {
                    return
                }
            }
        }
    }

    fun unpack(ints: IntArray) {
        val i = data.size
        var j = 0
        for (k in 0 until i - 1) {
            var l = data[k]
            for (m in 0 until valuesPerLong) {
                ints[j + m] = (l and mask).toInt()
                l = l shr bits
            }
            j += valuesPerLong
        }
        val k = size - j
        if (k > 0) {
            var l = data[i - 1]
            for (m in 0 until k) {
                ints[j + m] = (l and mask).toInt()
                l = l shr bits
            }
        }
    }

    fun copy(): SimpleBitStorage {
        return SimpleBitStorage(bits, size, data.clone())
    }

    class InitializationException internal constructor(message: String) : RuntimeException(message)

    internal object SimpleBitStorageSerializer : KSerializer<SimpleBitStorage> {
        @Serializable
        @SerialName("SimpleBitStorage")
        internal class SimpleBitStorageSurrogate(val bits: Int, val size: Int, val data: LongArray)

        override val descriptor = SimpleBitStorageSurrogate.serializer().descriptor

        override fun serialize(encoder: Encoder, value: SimpleBitStorage) = encoder.encodeSerializableValue(
            SimpleBitStorageSurrogate.serializer(),
            SimpleBitStorageSurrogate(value.bits, value.size, value.data)
        )

        override fun deserialize(decoder: Decoder) =
            decoder.decodeSerializableValue(SimpleBitStorageSurrogate.serializer())
                .let { SimpleBitStorage(it.bits, it.size, it.data) }
    }

    companion object {
        private val MAGIC = intArrayOf(
            -1,
            -1,
            0, Int.MIN_VALUE,
            0,
            0,
            1431655765,
            1431655765,
            0, Int.MIN_VALUE,
            0,
            1,
            858993459,
            858993459,
            0,
            715827882,
            715827882,
            0,
            613566756,
            613566756,
            0, Int.MIN_VALUE,
            0,
            2,
            477218588,
            477218588,
            0,
            429496729,
            429496729,
            0,
            390451572,
            390451572,
            0,
            357913941,
            357913941,
            0,
            330382099,
            330382099,
            0,
            306783378,
            306783378,
            0,
            286331153,
            286331153,
            0, Int.MIN_VALUE,
            0,
            3,
            252645135,
            252645135,
            0,
            238609294,
            238609294,
            0,
            226050910,
            226050910,
            0,
            214748364,
            214748364,
            0,
            204522252,
            204522252,
            0,
            195225786,
            195225786,
            0,
            186737708,
            186737708,
            0,
            178956970,
            178956970,
            0,
            171798691,
            171798691,
            0,
            165191049,
            165191049,
            0,
            159072862,
            159072862,
            0,
            153391689,
            153391689,
            0,
            148102320,
            148102320,
            0,
            143165576,
            143165576,
            0,
            138547332,
            138547332,
            0, Int.MIN_VALUE,
            0,
            4,
            130150524,
            130150524,
            0,
            126322567,
            126322567,
            0,
            122713351,
            122713351,
            0,
            119304647,
            119304647,
            0,
            116080197,
            116080197,
            0,
            113025455,
            113025455,
            0,
            110127366,
            110127366,
            0,
            107374182,
            107374182,
            0,
            104755299,
            104755299,
            0,
            102261126,
            102261126,
            0,
            99882960,
            99882960,
            0,
            97612893,
            97612893,
            0,
            95443717,
            95443717,
            0,
            93368854,
            93368854,
            0,
            91382282,
            91382282,
            0,
            89478485,
            89478485,
            0,
            87652393,
            87652393,
            0,
            85899345,
            85899345,
            0,
            84215045,
            84215045,
            0,
            82595524,
            82595524,
            0,
            81037118,
            81037118,
            0,
            79536431,
            79536431,
            0,
            78090314,
            78090314,
            0,
            76695844,
            76695844,
            0,
            75350303,
            75350303,
            0,
            74051160,
            74051160,
            0,
            72796055,
            72796055,
            0,
            71582788,
            71582788,
            0,
            70409299,
            70409299,
            0,
            69273666,
            69273666,
            0,
            68174084,
            68174084,
            0, Int.MIN_VALUE,
            0,
            5
        )
    }
}