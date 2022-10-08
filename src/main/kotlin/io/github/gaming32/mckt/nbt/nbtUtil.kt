package io.github.gaming32.mckt.nbt

import net.kyori.adventure.nbt.BinaryTagType
import net.kyori.adventure.nbt.CompoundBinaryTag

operator fun CompoundBinaryTag.contains(key: String) = key in keySet()

fun CompoundBinaryTag.contains(key: String, type: BinaryTagType<*>) = this[key]?.type() == type
