package io.github.gaming32.mckt

import net.benwoodworth.knbt.Nbt
import net.benwoodworth.knbt.NbtCompression
import net.benwoodworth.knbt.NbtVariant
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.slf4j.LoggerFactory
import org.slf4j.helpers.Util

val NBT_FORMAT = Nbt {
    variant = NbtVariant.Java
    compression = NbtCompression.None
}

val USERNAME_REGEX = Regex("^\\w{2,16}\$")

fun getLogger() = LoggerFactory.getLogger(Util.getCallingClass())

fun Component.plainText() = PlainTextComponentSerializer.plainText().serialize(this)
