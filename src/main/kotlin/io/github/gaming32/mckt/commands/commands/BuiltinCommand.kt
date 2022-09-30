package io.github.gaming32.mckt.commands.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import io.github.gaming32.mckt.commands.CommandSource
import net.kyori.adventure.text.Component

sealed interface BuiltinCommand {
    val helpText: Component

    val aliases get() = listOf<String>()

    fun buildTree(): LiteralArgumentBuilder<CommandSource>
}
