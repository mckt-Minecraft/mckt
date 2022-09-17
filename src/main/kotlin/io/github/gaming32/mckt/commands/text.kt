package io.github.gaming32.mckt.commands

import com.mojang.brigadier.Message
import com.mojang.brigadier.exceptions.CommandSyntaxException
import io.github.gaming32.mckt.plainText
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

class TextWrapper(val text: Component) : Message {
    override fun getString() = text.plainText()
    override fun toString() = text.toString()
}

fun Message.unwrap() = if (this is TextWrapper) text else Component.text(string)
fun Component.wrap() = TextWrapper(this)

val CommandSyntaxException.textMessage get() =
    Component.text { builder ->
        builder.color(NamedTextColor.RED)
        builder.append(rawMessage.unwrap())
        context?.let {
            builder.append(Component.text(" at position $cursor: $it"))
        }
    }
