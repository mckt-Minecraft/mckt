package io.github.gaming32.mckt

import kotlinx.serialization.json.Json
import net.benwoodworth.knbt.Nbt
import net.benwoodworth.knbt.NbtCompression
import net.benwoodworth.knbt.NbtVariant
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.slf4j.LoggerFactory
import org.slf4j.helpers.Util
import java.lang.IllegalArgumentException
import kotlin.math.max

val NETWORK_NBT = Nbt {
    variant = NbtVariant.Java
    compression = NbtCompression.None
}

val SAVE_NBT = Nbt {
    variant = NbtVariant.Java
    compression = NbtCompression.Gzip
}

val PRETTY_JSON = Json {
    prettyPrint = true
    encodeDefaults = true
    isLenient = true
    coerceInputValues = true
}

val USERNAME_REGEX = Regex("^\\w{2,16}\$")

fun getLogger() = LoggerFactory.getLogger(Util.getCallingClass())!!

fun Component.plainText() = PlainTextComponentSerializer.plainText().serialize(this)

fun Int.squared() = this * this

inline fun spiralLoop(w: Int, h: Int, action: (x: Int, y: Int) -> Unit) {
    var x = 0
    var y = 0
    var dx = 0
    var dy = -1
    repeat(max(w, h).squared()) {
        if (-w / 2 < x && x <= w / 2 && -h / 2 < y && y <= h / 2) {
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
