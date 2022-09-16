package io.github.gaming32.mckt.commands

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import net.kyori.adventure.text.Component

val ONLY_INTS_EXCEPTION = SimpleCommandExceptionType(Component.translatable("argument.range.ints").wrap())

class FloatRange(val min: Float?, val max: Float?) {
    companion object Constructors {
        val ANY = FloatRange(null, null)

        fun exactly(value: Float) = FloatRange(value, value)
        fun between(min: Float, max: Float) = FloatRange(min, max)
        fun atLeast(min: Float) = FloatRange(min, null)
        fun atMost(max: Float) = FloatRange(null, max)
    }

    operator fun contains(value: Float) =
        if (min != null && max != null && min > max && min > value && max < value) {
            false
        } else if (min != null && min > value) {
            false
        } else {
            max == null || max >= value
        }

    fun containsSquared(value: Double) =
        if (
            min != null && max != null && min > max &&
            (min * min).toDouble() > value && (max * max).toDouble() < value
        ) {
            false
        } else if (min != null && (min * min).toDouble() > value) {
            false
        } else {
            max == null || (max * max).toDouble() >= value
        }
}

sealed class NumberRange<T : Number>(val min: T?, val max: T?) {
    companion object {
        val EMPTY_EXCEPTION = SimpleCommandExceptionType(Component.translatable("argument.range.empty").wrap())
        val SWAPPED_EXCEPTION = SimpleCommandExceptionType(Component.translatable("argument.range.swapped").wrap())

        @JvmStatic
        @PublishedApi
        internal inline fun <T : Number, R : NumberRange<T>> parse(
            reader: StringReader,
            commandFactory: (StringReader, T?, T?) -> R,
            converter: (String) -> T,
            exceptionType: () -> DynamicCommandExceptionType,
            mapper: (T) -> T
        ): R {
            if (!reader.canRead()) {
                throw EMPTY_EXCEPTION.createWithContext(reader)
            }
            val cursor = reader.cursor
            try {
                val min = map(fromStringReader(reader, converter, exceptionType), mapper)
                val max = if (reader.canRead(2) && reader.peek() == '.' && reader.peek(1) == '.') {
                    reader.skip()
                    reader.skip()
                    map(fromStringReader(reader, converter, exceptionType), mapper)
                } else {
                    min
                }

                if (min == null && max == null) {
                    throw EMPTY_EXCEPTION.createWithContext(reader)
                }
                return commandFactory(reader, min, max)
            } catch (e: CommandSyntaxException) {
                reader.cursor = cursor
                throw CommandSyntaxException(e.type, e.rawMessage, e.input, cursor)
            }
        }

        @PublishedApi
        internal inline fun <T : Number> fromStringReader(
            reader: StringReader,
            converter: (String) -> T,
            exceptionType: () -> DynamicCommandExceptionType
        ): T? {
            val cursor = reader.cursor
            while (reader.canRead() && isNextCharValid(reader)) {
                reader.skip()
            }

            val value = reader.string.substring(cursor, reader.cursor)
            if (value.isEmpty()) return null
            try {
                return converter(value)
            } catch (e: NumberFormatException) {
                throw exceptionType().createWithContext(reader, value)
            }
        }

        @PublishedApi
        internal fun isNextCharValid(reader: StringReader): Boolean {
            val c = reader.peek()
            if ((c < '0' || c > '9') && c != '-') {
                if (c != '.') {
                    return false
                } else {
                    return !reader.canRead(2) || reader.peek(1) != '.'
                }
            }
            return true
        }

        @PublishedApi
        internal inline fun <T> map(value: T?, function: (T) -> T) = value?.let { function(it) }
    }

    val isDummy get() = min == null && max == null

    override fun toString() = "${min?.toString() ?: ""}..${max?.toString() ?: ""}"
}

class DoubleRange(min: Double?, max: Double?) : NumberRange<Double>(min, max) {
    private val minSquared = min?.times(min)
    private val maxSquared = max?.times(max)

    companion object Constructors {
        val ANY = DoubleRange(null, null)

        fun exactly(value: Double) = DoubleRange(value, value)
        fun between(min: Double, max: Double) = DoubleRange(min, max)
        fun atLeast(min: Double) = DoubleRange(min, null)
        fun atMost(max: Double) = DoubleRange(null, max)

        fun parse(reader: StringReader) = parse(reader) { it }

        inline fun parse(reader: StringReader, mapper: (Double) -> Double) = parse(
            reader,
            { _, min, max ->
                if (min != null && max != null && min > max) {
                    throw SWAPPED_EXCEPTION.createWithContext(reader)
                }
                DoubleRange(min, max)
            },
            String::toDouble,
            CommandSyntaxException.BUILT_IN_EXCEPTIONS::readerInvalidDouble,
            mapper
        )
    }

    operator fun contains(value: Double) =
        if (min != null && min > value) {
            false
        } else {
            max == null || max >= value
        }

    fun containsSquared(value: Double) =
        if (minSquared != null && minSquared > value) {
            false
        } else {
            maxSquared == null || maxSquared >= value
        }
}

class MinecraftIntRange(min: Int?, max: Int?) : NumberRange<Int>(min, max) {
    private val minSquared = min?.toUInt()?.let { it * it }
    private val maxSquared = max?.toUInt()?.let { it * it }

    companion object Constructors {
        val ANY = MinecraftIntRange(null, null)

        fun exactly(value: Int) = MinecraftIntRange(value, value)
        fun between(min: Int, max: Int) = MinecraftIntRange(min, max)
        fun atLeast(min: Int) = MinecraftIntRange(min, null)
        fun atMost(max: Int) = MinecraftIntRange(null, max)

        fun parse(reader: StringReader) = parse(reader) { it }

        inline fun parse(reader: StringReader, mapper: (Int) -> Int) = parse(
            reader,
            { _, min, max ->
                if (min != null && max != null && min > max) {
                    throw SWAPPED_EXCEPTION.createWithContext(reader)
                }
                MinecraftIntRange(min, max)
            },
            String::toInt,
            CommandSyntaxException.BUILT_IN_EXCEPTIONS::readerInvalidInt,
            mapper
        )
    }

    operator fun contains(value: Int) =
        if (min != null && min > value) {
            false
        } else {
            max == null || max >= value
        }

    fun containsSquared(value: Int) =
        if (minSquared != null && minSquared > value.toUInt()) {
            false
        } else {
            maxSquared == null || maxSquared >= value.toUInt()
        }
}

infix fun Double.inSquared(range: FloatRange) = range.containsSquared(this)
infix fun Double.inSquared(range: DoubleRange) = range.containsSquared(this)
infix fun Int.inSquared(range: MinecraftIntRange) = range.containsSquared(this)
