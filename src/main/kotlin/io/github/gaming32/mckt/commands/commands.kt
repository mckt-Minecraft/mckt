package io.github.gaming32.mckt.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import io.github.gaming32.mckt.getLogger
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.format.NamedTextColor

private val LOGGER = getLogger()

suspend fun CommandSource.runCommand(command: String, dispatcher: CommandDispatcher<CommandSource>) {
    try {
        dispatcher.execute(command, this)
    } catch (e: CommandSyntaxException) {
        reply(e.textMessage)
    } catch (e: Exception) {
        LOGGER.error("Internal command error", e)
        if (this !is ConsoleCommandSource) reply(Component.join(
            JoinConfiguration.newlines(),
            Component.translatable("command.failed", NamedTextColor.RED),
            Component.text(e.toString(), NamedTextColor.RED)
        ))
    }
}

fun <S, T : ArgumentBuilder<S, T>> ArgumentBuilder<S, T>.executesSuspend(block: suspend CommandContext<S>.() -> Int) =
    executes { ctx -> runBlocking { ctx.block() } }!!
