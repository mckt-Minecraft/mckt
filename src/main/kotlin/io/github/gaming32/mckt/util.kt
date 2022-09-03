package io.github.gaming32.mckt

import kotlinx.serialization.json.Json
import net.benwoodworth.knbt.Nbt
import net.benwoodworth.knbt.NbtCompression
import net.benwoodworth.knbt.NbtVariant
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.slf4j.LoggerFactory
import org.slf4j.helpers.Util

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
}

val USERNAME_REGEX = Regex("^\\w{2,16}\$")

fun getLogger() = LoggerFactory.getLogger(Util.getCallingClass())

fun Component.plainText() = PlainTextComponentSerializer.plainText().serialize(this)
