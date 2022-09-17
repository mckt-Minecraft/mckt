package io.github.gaming32.mckt.commands.arguments

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext

fun CommandContext<*>.getString(name: String) = StringArgumentType.getString(this, name)!!
fun CommandContext<*>.getInteger(name: String) = IntegerArgumentType.getInteger(this, name)