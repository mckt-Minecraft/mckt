package io.github.gaming32.mckt

import io.github.gaming32.mckt.objects.Identifier
import it.unimi.dsi.fastutil.ints.IntIntPair
import kotlinx.serialization.json.Json
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.flattener.ComponentFlattener
import net.kyori.adventure.text.flattener.FlattenerListener
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.jline.utils.AttributedStringBuilder
import org.jline.utils.AttributedStyle
import org.slf4j.LoggerFactory
import org.slf4j.helpers.Util
import java.io.OutputStream
import java.math.BigInteger
import java.util.*
import kotlin.collections.ArrayDeque
import kotlin.math.max
import kotlin.random.Random

val TRANSLATABLE_FLATTENER = ComponentFlattener.basic().toBuilder().apply {
    complexMapper(TranslatableComponent::class.java) { component, handler ->
        val translation = DEFAULT_TRANSLATIONS[component.key()]
            ?: return@complexMapper handler.accept(Component.text(component.key()))
        val parts = translation.split("%s", limit = component.args().size + 1)
        if (parts.isEmpty()) return@complexMapper
        handler.accept(Component.text(parts[0].replace("%%", "%")))
        for (i in 1 until parts.size) {
            handler.accept(component.args()[i - 1])
            handler.accept(Component.text(parts[i].replace("%%", "%")))
        }
    }
}.build()

val PLAIN_TEXT_SERIALIZER = PlainTextComponentSerializer.builder().apply {
    flattener(TRANSLATABLE_FLATTENER)
}.build()

val ADVENTURE_TO_JLINE_STYLES = mapOf(
    TextDecoration.BOLD to Triple(
        AttributedStyle::bold,
        AttributedStyle::boldOff,
        AttributedStyle::boldDefault
    ),
    TextDecoration.ITALIC to Triple(
        AttributedStyle::italic,
        AttributedStyle::italicOff,
        AttributedStyle::italicDefault
    ),
    TextDecoration.UNDERLINED to Triple(
        AttributedStyle::underline,
        AttributedStyle::underlineOff,
        AttributedStyle::underlineDefault
    ),
    TextDecoration.STRIKETHROUGH to Triple(
        AttributedStyle::crossedOut,
        AttributedStyle::crossedOutOff,
        AttributedStyle::crossedOutDefault
    ),
    TextDecoration.OBFUSCATED to Triple(
        AttributedStyle::inverse,
        AttributedStyle::inverseOff,
        AttributedStyle::inverseDefault
    )
)

val PRETTY_JSON = Json {
    prettyPrint = true
    encodeDefaults = true
    isLenient = true
    coerceInputValues = true
    ignoreUnknownKeys = true
}

val USERNAME_REGEX = Regex("^\\w{1,16}\$")

fun getLogger() = LoggerFactory.getLogger(Util.getCallingClass())!!

fun Component.plainText() = PLAIN_TEXT_SERIALIZER.serialize(this)

fun Component.attributedTextTo(builder: AttributedStringBuilder) =
    TRANSLATABLE_FLATTENER.flatten(this, object : FlattenerListener {
        val styleStack = ArrayDeque<Style>()

        override fun pushStyle(style: Style) {
            if (!style.isEmpty) { // No style change, nothing to do
                builder.style {
                    var newStyle = it
                    ADVENTURE_TO_JLINE_STYLES.forEach { adventure, (jlineOn, jlineOff, _) ->
                        if (style.decoration(adventure) != TextDecoration.State.NOT_SET) {
                            newStyle = if (style.hasDecoration(adventure)) jlineOn(newStyle) else jlineOff(newStyle)
                        }
                    }
                    style.color()?.let { color ->
                        newStyle = newStyle.foreground(color.red(), color.green(), color.blue())
                    }
                    newStyle
                }
            }
            val top = styleStack.lastOrNull()
            styleStack.addLast(if (top != null) style.merge(top) else style)
        }

        override fun component(text: String) {
            builder.append(text)
        }

        override fun popStyle(style: Style) {
            styleStack.removeLast()
            val top = styleStack.lastOrNull()
            if (top != null) {
                if (!style.isEmpty) { // Nothing had changed, thus nothing to do
                    builder.style {
                        var newStyle = it
                        ADVENTURE_TO_JLINE_STYLES.forEach { adventure, (jlineOn, jlineOff, jlineDefault) ->
                            val state = top.decoration(adventure)
                            if (state == TextDecoration.State.NOT_SET) {
                                newStyle = jlineDefault(newStyle)
                            } else if (state != style.decoration(adventure)) {
                                newStyle = if (state == TextDecoration.State.TRUE) {
                                    jlineOn(newStyle)
                                } else {
                                    jlineOff(newStyle)
                                }
                            }
                        }
                        val color = top.color()
                        if (color == null) {
                            newStyle = newStyle.foregroundDefault()
                        } else if (color != style.color()) {
                            newStyle = newStyle.foreground(color.red(), color.green(), color.blue())
                        }
                        newStyle
                    }
                }
            } else {
                builder.style(AttributedStyle.DEFAULT)
            }
        }
    })

fun Component.attributedText() = AttributedStringBuilder().apply {
    attributedTextTo(this)
}.toAttributedString()!!

fun Int.squared() = this * this

inline fun spiralLoop(w: Int, h: Int, action: (x: Int, y: Int) -> Unit) {
    var x = 0
    var y = 0
    var dx = 0
    var dy = -1
    repeat(max(w, h).squared()) {
        if (-w / 2.0 < x && x <= w / 2.0 && -h / 2.0 < y && y <= h / 2.0) {
            action(x, y)
        }
        if (x == y || (x < 0 && x == -y) || (x > 0 && x == 1 - y)) {
            val temp = dx
            dx = -dy
            dy = temp
        }
        x += dx
        y += dy
    }
}

inline fun <reified T : Enum<T>> enumValueOfOrNull(name: String) = try {
    enumValueOf<T>(name)
} catch (e: IllegalArgumentException) {
    null
}

fun CharSequence.capitalize() = if (isEmpty()) "" else "${this[0].titlecase()}${this.substring(1).lowercase()}"

fun <K, V> Map<K, V>.invert() = asSequence().associate { (k, v) -> v to k }

fun <K, V, M : MutableMap<in V, in K>> Map<K, V>.invertTo(destination: M) =
    asSequence().associateTo(destination) { (k, v) -> v to k }

fun ByteArray.toHexString(): String = joinToString("") { it.toUByte().toString(radix = 16).padStart(2, '0') }

fun <K, V> Map<K, V>.toTypedArray() = iterator().let { iter -> Array(size) { iter.next().run { key to value } } }

inline fun <reified K : Enum<K>, V> enumMapOf(): MutableMap<K, V> = EnumMap(K::class.java)

fun <K : Enum<K>, V> enumMapOf(vararg elements: Pair<K, V>): MutableMap<K, V> {
    require(elements.isNotEmpty()) { "At least one element is required" }
    val result = EnumMap<K, V>(elements[0].first.javaClass)
    elements.forEach { result[it.first] = it.second }
    return result
}

operator fun IntIntPair.component1() = firstInt()
operator fun IntIntPair.component2() = secondInt()

inline infix fun <T> ((T) -> Boolean).and(crossinline other: (T) -> Boolean) = { it: T -> this(it) && other(it) }
inline infix fun <T> ((T) -> Boolean).or(crossinline other: (T) -> Boolean) = { it: T -> this(it) || other(it) }
@Suppress("NOTHING_TO_INLINE")
inline operator fun <T> ((T) -> Boolean).not() = { it: T -> !this(it) }

fun Double.squared() = this * this

fun wrapDegrees(degrees: Float) = (degrees % 360f).let { when {
    it >= 180f -> it - 360f
    it < -180f -> it + 360f
    else -> it
} }

fun Any?.toText() = if (this is ComponentLike) asComponent() else Component.text(toString())

fun uuidFromAnyString(s: String) = UUID.fromString(
    s.replaceFirst (
        "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
        "$1-$2-$3-$4-$5"
    )
)!!

inline fun <reified T> Any?.castOrNull() = this as? T

inline fun <reified T> Any?.cast() = this as T

fun Random.nextTriangular(mode: Double, deviation: Double) = mode + deviation * (nextDouble() - nextDouble())

fun setColorAlpha(rgb: Int, alpha: Int): Int {
    require(alpha in 0..255) { "Alpha not in range 0..255" }
    return rgb and 0xffffff or (alpha shl 24)
}

fun Long.coerceToInt() = when {
    this > Int.MAX_VALUE.toLong() -> Int.MAX_VALUE
    this < Int.MIN_VALUE.toLong() -> Int.MIN_VALUE
    else -> this.toInt()
}

fun BigInteger.coerceToInt() = when {
    this > Int.MAX_VALUE.toBigInteger() -> Int.MAX_VALUE
    this < Int.MIN_VALUE.toBigInteger() -> Int.MIN_VALUE
    else -> this.toInt()
}

fun <T> MutableList<T>.swap(idx1: Int, idx2: Int) {
    if (idx1 != idx2) {
        this[idx1] = set(idx2, this[idx1])
    }
}

fun Key.toIdentifier() = Identifier(namespace(), value())

fun Identifier.toKey() = Key.key(namespace, value)

class NonCloseableOutputStream(private val inner: OutputStream) : OutputStream() {
    override fun write(b: Int) = inner.write(b)

    override fun write(b: ByteArray) = inner.write(b)

    override fun write(b: ByteArray, off: Int, len: Int) = inner.write(b, off, len)
}
