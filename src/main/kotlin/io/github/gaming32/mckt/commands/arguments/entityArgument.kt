package io.github.gaming32.mckt.commands.arguments

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import io.github.gaming32.mckt.commands.CommandSource
import io.github.gaming32.mckt.commands.EntitySelector
import io.github.gaming32.mckt.commands.EntitySelectorReader
import io.github.gaming32.mckt.commands.wrap
import net.kyori.adventure.text.Component


data class EntityArgumentType(
    val singleTarget: Boolean,
    val playersOnly: Boolean
) : ArgumentType<EntitySelector> {
    companion object {
        private val EXAMPLES = listOf("Player", "0123", "@e", "@e[type=foo]", "dd12be42-52a9-4a91-a8a1-11c01849e498")
        val TOO_MANY_ENTITIES_EXCEPTION = SimpleCommandExceptionType(
            Component.translatable("argument.entity.toomany").wrap()
        )
        val TOO_MANY_PLAYERS_EXCEPTION = SimpleCommandExceptionType(
            Component.translatable("argument.player.toomany").wrap()
        )
        val PLAYER_SELECTOR_HAS_ENTITIES_EXCEPTION = SimpleCommandExceptionType(
            Component.translatable("argument.player.entities").wrap()
        )
        val ENTITY_NOT_FOUND_EXCEPTION = SimpleCommandExceptionType(
            Component.translatable("argument.entity.notfound.entity").wrap()
        )
        val PLAYER_NOT_FOUND_EXCEPTION = SimpleCommandExceptionType(
            Component.translatable("argument.entity.notfound.player").wrap()
        )
        val NOT_ALLOWED_EXCEPTION = SimpleCommandExceptionType(
            Component.translatable("argument.entity.selector.not_allowed").wrap()
        )
    }

    override fun parse(reader: StringReader): EntitySelector {
        val selectorReader = EntitySelectorReader(reader)
        val selector = selectorReader.read()
        if (selector.limit > 1 && singleTarget) {
            reader.cursor = 0
            if (playersOnly) {
                throw TOO_MANY_PLAYERS_EXCEPTION.createWithContext(reader)
            } else {
                throw TOO_MANY_ENTITIES_EXCEPTION.createWithContext(reader)
            }
        } else if (selector.includesNonPlayers && playersOnly && !selector.senderOnly) {
            reader.cursor = 0
            throw PLAYER_SELECTOR_HAS_ENTITIES_EXCEPTION.createWithContext(reader)
        }
        return selector
    }

    override fun getExamples() = EXAMPLES
}

fun entity() = EntityArgumentType(singleTarget = true, playersOnly = false)

fun CommandContext<CommandSource>.getEntity(name: String) =
    getArgument(name, EntitySelector::class.java).getEntity(source)

fun entities() = EntityArgumentType(singleTarget = false, playersOnly = false)

fun CommandContext<CommandSource>.getEntities(name: String) = getOptionalEntities(name).also {
    if (it.isEmpty()) {
        throw EntityArgumentType.ENTITY_NOT_FOUND_EXCEPTION.create()
    }
}

fun CommandContext<CommandSource>.getOptionalEntities(name: String) =
    getArgument(name, EntitySelector::class.java).getEntities(source)

fun player() = EntityArgumentType(singleTarget = true, playersOnly = true)

fun CommandContext<CommandSource>.getPlayer(name: String) =
    getArgument(name, EntitySelector::class.java).getPlayer(source)

fun players() = EntityArgumentType(singleTarget = false, playersOnly = true)

fun CommandContext<CommandSource>.getPlayers(name: String) = getOptionalPlayers(name).also {
    if (it.isEmpty()) {
        throw EntityArgumentType.ENTITY_NOT_FOUND_EXCEPTION.create()
    }
}

fun CommandContext<CommandSource>.getOptionalPlayers(name: String) =
    getArgument(name, EntitySelector::class.java).getPlayers(source)
