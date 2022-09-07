package io.github.gaming32.mckt.commands

import io.github.gaming32.mckt.PlayClient
import io.github.gaming32.mckt.getLogger
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import java.lang.Exception

private val LOGGER = getLogger()

private val commands = mutableMapOf<String, Command>()
val COMMANDS: Map<String, Command> get() =
    commands // Public getter gets immutable-type map. It's still mutable underneath, though

abstract class Command(val name: String, val description: Component?, val minPermission: Int) {
    abstract suspend fun call(sender: CommandSender, args: String): Boolean
}

fun registerCommand(command: Command) =
    commands.putIfAbsent(command.name, command)?.let { existing ->
        throw IllegalArgumentException("Command ${existing.name} already exists")
    }.let { command }

inline fun registerCommandPermCheck(
    name: String,
    description: Component? = null,
    permission: Int = 0,
    crossinline executor: suspend Command.(sender: CommandSender, args: String) -> Boolean
) = registerCommand(object : Command(name, description, permission) {
    override suspend fun call(sender: CommandSender, args: String) = executor(sender, args)
})

inline fun registerCommand(
    name: String,
    description: Component? = null,
    permission: Int = 0,
    crossinline executor: suspend Command.(sender: CommandSender, args: String) -> Unit
) = registerCommandPermCheck(name, description, permission) { sender, args ->
    executor(sender, args)
    true
}

fun CommandSender.evaluateClient(name: String): PlayClient? = server.clients[name]

suspend fun CommandSender.runCommand(command: String) {
    val (baseCommand, rest) = if (' ' in command) {
        command.split(' ', limit = 2)
    } else {
        listOf(command, "")
    }
    try {
        COMMANDS[baseCommand]?.let { commandToRun ->
            if (commandToRun.minPermission > operator) {
                return@let null
            }
            if (commandToRun.call(this, rest)) Unit else null
        } ?: reply(Component.translatable("commands.help.failed", NamedTextColor.RED))
    } catch (e: Exception) {
        LOGGER.error("Internal command error", e)
        if (this !is ConsoleCommandSender) reply(
            Component.text("Internal command error", NamedTextColor.DARK_RED)
        )
    }
}
