package io.github.gaming32.mckt.commands

import com.mojang.brigadier.Message
import io.github.gaming32.mckt.plainText
import net.kyori.adventure.text.Component

class TextWrapper(val text: Component) : Message {
    override fun getString() = text.plainText()
    override fun toString() = text.toString()
}

fun Message.unwrap() = if (this is TextWrapper) text else Component.text(string)
fun Component.wrap() = TextWrapper(this)
