package io.github.gaming32.mckt.config.frontendapi

import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.util.HSVLike

fun color(named: String) =
    if (named.startsWith("#")) {
        TextColor.fromCSSHexString(named)
    } else {
        NamedTextColor.NAMES.value(named)
    }

fun hsv(h: Float, s: Float, v: Float) = TextColor.color(HSVLike.hsvLike(h, s, v))
