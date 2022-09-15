package io.github.gaming32.mckt.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import io.github.gaming32.mckt.PlayClient
import io.github.gaming32.mckt.getLogger
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

private val LOGGER = getLogger()

private val commands = mutableMapOf<String, Command>()
val COMMANDS: Map<String, Command> get() =
    commands // Public getter gets immutable-type map. It's still mutable underneath, though

abstract class Command(val name: String, val description: Component?, val minPermission: Int) {
    abstract suspend fun call(sender: CommandSource, args: String): Boolean
}

fun registerCommand(command: Command) =
    commands.putIfAbsent(command.name, command)?.let { existing ->
        throw IllegalArgumentException("Command ${existing.name} already exists")
    }.let { command }

inline fun registerCommandPermCheck(
    name: String,
    description: Component? = null,
    permission: Int = 0,
    crossinline executor: suspend Command.(sender: CommandSource, args: String) -> Boolean
) = registerCommand(object : Command(name, description, permission) {
    override suspend fun call(sender: CommandSource, args: String) = executor(sender, args)
})

inline fun registerCommand(
    name: String,
    description: Component? = null,
    permission: Int = 0,
    crossinline executor: suspend Command.(sender: CommandSource, args: String) -> Unit
) = registerCommandPermCheck(name, description, permission) { sender, args ->
    executor(sender, args)
    true
}

fun CommandSource.evaluateClient(name: String): PlayClient? = server.clients[name]

suspend fun CommandSource.runCommand(command: String, dispatcher: CommandDispatcher<CommandSource>) {
    val (baseCommand, rest) = if (' ' in command) {
        command.split(' ', limit = 2)
    } else {
        listOf(command, "")
    }
    if (!run {
        try {
            COMMANDS[baseCommand]?.let { commandToRun ->
                if (commandToRun.minPermission > operator) {
                    return@let false
                }
                commandToRun.call(this, rest)
            } == true
        } catch (e: Exception) {
            LOGGER.error("Internal command error", e)
            if (this !is ConsoleCommandSource) reply(
                Component.translatable("command.failed", NamedTextColor.DARK_RED)
            )
            true
        }
    }) {
        try {
            dispatcher.execute(command, this)
        } catch (e: CommandSyntaxException) {
            reply(Component.text(e.localizedMessage, NamedTextColor.RED))
        } catch (e: Exception) {
            LOGGER.error("Internal command error", e)
            if (this !is ConsoleCommandSource) reply(
                Component.translatable("command.failed", NamedTextColor.DARK_RED)
            )
        }
    }
}

fun <S, T : ArgumentBuilder<S, T>> ArgumentBuilder<S, T>.executesSuspend(block: suspend CommandContext<S>.() -> Int) =
    executes { ctx -> runBlocking { ctx.block() } }!!
