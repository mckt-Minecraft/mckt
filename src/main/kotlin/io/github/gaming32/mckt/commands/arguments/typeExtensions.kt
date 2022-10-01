package io.github.gaming32.mckt.commands.arguments

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.LongArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext

fun CommandContext<*>.getString(name: String) = StringArgumentType.getString(this, name)!!
fun CommandContext<*>.getInt(name: String) = IntegerArgumentType.getInteger(this, name)
fun CommandContext<*>.getLong(name: String) = LongArgumentType.getLong(this, name)
