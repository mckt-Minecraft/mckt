package io.github.gaming32.mckt.commands

import com.mojang.brigadier.Message
import com.mojang.brigadier.exceptions.CommandSyntaxException
import io.github.gaming32.mckt.plainText
import io.github.gaming32.mckt.toText
import net.kyori.adventure.extra.kotlin.text
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

class TextWrapper(val text: Component) : Message {
    override fun getString() = text.plainText()
    override fun toString() = text.toString()
}

fun Message.unwrap() = if (this is TextWrapper) text else Component.text(string)
fun Component.wrap() = TextWrapper(this)

val CommandSyntaxException.textMessage get() =
    text {
        color(NamedTextColor.RED)
        append(rawMessage.unwrap())
        context?.let {
            append(Component.text(" at position $cursor: $it"))
        }
    }

val Throwable.textMessage get() = if (this is CommandSyntaxException) textMessage else localizedMessage.toText()
