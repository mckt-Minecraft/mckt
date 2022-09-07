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
    abstract suspend fun call(sender: CommandSender, args: String)

    protected suspend fun validatePermission(sender: CommandSender): Boolean {
        if (sender.operator < minPermission) {
            sender.noPermission()
            return false
        }
        return true
    }
}

fun registerCommand(command: Command) =
    commands.putIfAbsent(command.name, command)?.let { existing ->
        throw IllegalArgumentException("Command ${existing.name} already exists")
    }.let { command }

inline fun registerCommand(
    name: String,
    description: Component? = null,
    permission: Int = 0,
    crossinline executor: suspend Command.(sender: CommandSender, args: String) -> Unit
) = registerCommand(object : Command(name, description, permission) {
    override suspend fun call(sender: CommandSender, args: String) = executor(sender, args)
})

fun CommandSender.evaluateClient(name: String): PlayClient? = server.clients[name]

suspend fun CommandSender.runCommand(command: String) {
    val (baseCommand, rest) = if (' ' in command) {
        command.split(' ', limit = 2)
    } else {
        listOf(command, "")
    }
    try {
        COMMANDS[baseCommand]?.call(this, rest) ?: reply( // TODO: check perms
            Component.text("Unknown command: $baseCommand", NamedTextColor.RED)
        )
    } catch (e: Exception) {
        LOGGER.error("Internal command error", e)
        if (this !is ConsoleCommandSender) reply(
            Component.text("Internal command error", NamedTextColor.DARK_RED)
        )
    }
}
