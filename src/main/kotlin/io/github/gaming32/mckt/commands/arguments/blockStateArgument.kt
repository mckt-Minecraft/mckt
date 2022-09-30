package io.github.gaming32.mckt.commands.arguments

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import io.github.gaming32.mckt.commands.CommandSource
import io.github.gaming32.mckt.objects.BlockState

object BlockStateArgumentType : ArgumentType<BlockState> {
    private val EXAMPLES = listOf("stone", "minecraft:stone", "stone[foo=bar]", "foo{bar=baz}")

    override fun parse(reader: StringReader) = BlockState.parse(reader)

    override fun getExamples() = EXAMPLES
}

fun CommandContext<CommandSource>.getBlockState(name: String) = getArgument(name, BlockState::class.java)!!
