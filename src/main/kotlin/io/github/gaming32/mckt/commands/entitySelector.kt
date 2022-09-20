package io.github.gaming32.mckt.commands

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import io.github.gaming32.mckt.PlayClient
import io.github.gaming32.mckt.and
import io.github.gaming32.mckt.commands.arguments.EntityArgumentType
import io.github.gaming32.mckt.objects.Box
import io.github.gaming32.mckt.objects.Vector3d
import io.github.gaming32.mckt.toText
import io.github.gaming32.mckt.wrapDegrees
import net.kyori.adventure.text.Component
import java.util.*
import kotlin.math.min

typealias EntitySorter = (Vector3d, MutableList<PlayClient>) -> Unit

data class EntitySelector(
    val limit: Int,
    val includesNonPlayers: Boolean,
    val localWorldOnly: Boolean,
    val basePredicate: (PlayClient) -> Boolean,
    val distance: DoubleRange,
    val positionOffset: (Vector3d) -> Vector3d,
    val box: Box?,
    val sorter: EntitySorter,
    val senderOnly: Boolean,
    val playerName: String?,
    val uuid: UUID?,
    val usesAt: Boolean
) {
    private fun checkPermission(source: CommandSource) {
        if (usesAt && !source.hasPermission(1)) {
            throw EntityArgumentType.NOT_ALLOWED_EXCEPTION.create()
        }
    }

    fun getEntity(source: CommandSource) = getEntities(source).let { entities -> when {
        entities.isEmpty() -> throw EntityArgumentType.ENTITY_NOT_FOUND_EXCEPTION.create()
        entities.size > 1 -> throw EntityArgumentType.TOO_MANY_ENTITIES_EXCEPTION.create()
        else -> entities[0]
    } }

    fun getEntities(source: CommandSource): List<PlayClient> {
        checkPermission(source)
        if (!includesNonPlayers) {
            return getPlayers(source)
        }
        // TODO: Implement non-player entities
        return getPlayers(source)
    }

    fun getPlayer(source: CommandSource) = getPlayers(source).let { players ->
        if (players.size != 1) {
            throw EntityArgumentType.PLAYER_NOT_FOUND_EXCEPTION.create()
        }
        players[0]
    }

    fun getPlayers(source: CommandSource): List<PlayClient> {
        checkPermission(source)
        if (playerName != null) {
            val player = source.server.getPlayerByName(playerName)
            return if (player == null) listOf() else listOf(player)
        }
        if (uuid != null) {
            val player = source.server.getPlayerByUuid(uuid)
            return if (player == null) listOf() else listOf(player)
        }
        val origin = positionOffset(source.position)
        val positionPredicate = getPositionPredicate(origin)
        if (senderOnly) {
            val sourceEntity = source.entity
            if (sourceEntity is PlayClient && positionPredicate(sourceEntity)) {
                return listOf(sourceEntity)
            }
            return listOf()
        } else {
            val result = source.server.clients.values.filterTo(mutableListOf(), positionPredicate)
            return sortAndTrimEntities(origin, result)
        }
    }

    private fun getPositionPredicate(origin: Vector3d): (PlayClient) -> Boolean {
        var predicate = basePredicate
        if (box != null) {
            val offsetAabb = box + origin
            predicate = predicate and { offsetAabb.intersects(it.boundingBox) }
        }
        if (!distance.isDummy) {
            predicate = predicate and { (it.position distanceToSquared origin) inSquared distance }
        }
        return predicate
    }

    private fun sortAndTrimEntities(origin: Vector3d, entities: MutableList<PlayClient>): List<PlayClient> {
        if (entities.size > 1) {
            sorter(origin, entities)
        }
        return entities.subList(0, min(limit, entities.size))
    }
}

class EntitySelectorReader(val reader: StringReader, private val atAllowed: Boolean = true) {
    companion object {
        val INVALID_ENTITY_EXCEPTION = SimpleCommandExceptionType(
            Component.translatable("argument.entity.invalid").wrap()
        )
        val UNKNOWN_SELECTOR_EXCEPTION = DynamicCommandExceptionType { selectorType ->
            Component.translatable("argument.entity.selector.unknown", selectorType.toText()).wrap()
        }
        val NOT_ALLOWED_EXCEPTION = SimpleCommandExceptionType(
            Component.translatable("argument.entity.selector.not_allowed").wrap()
        )
        val MISSING_EXCEPTION = SimpleCommandExceptionType(
            Component.translatable("argument.entity.selector.missing").wrap()
        )
        val UNTERMINATED_EXCEPTION = SimpleCommandExceptionType(
            Component.translatable("argument.entity.options.unterminated").wrap()
        )
        val VALUELESS_EXCEPTION = DynamicCommandExceptionType { option ->
            Component.translatable("argument.entity.options.valueless", option.toText()).wrap()
        }
        val ARBITRARY: EntitySorter = { _, _ -> }
        val NEAREST: EntitySorter = { origin, entities ->
            entities.sortBy { it.position.distanceToSquared(origin) }
        }
        val FURTHEST: EntitySorter = { origin, entities ->
            entities.sortByDescending { it.position.distanceToSquared(origin) }
        }
        val RANDOM: EntitySorter = { _, entities -> entities.shuffle() }
    }

    var limit = 0
    var includesNonPlayers = false
    var localWorldOnly = false
    var distance: DoubleRange = DoubleRange.ANY
    var levelRange: MinecraftIntRange = MinecraftIntRange.ANY
    var x: Double? = null
    var y: Double? = null
    var z: Double? = null
    var dx: Double? = null
    var dy: Double? = null
    var dz: Double? = null
    var pitchRange: FloatRange = FloatRange.ANY
    var yawRange: FloatRange = FloatRange.ANY
    var predicate: (PlayClient) -> Boolean = { true }
    var sorter: EntitySorter = ARBITRARY
    var senderOnly = false
    var playerName: String? = null
    var startCursor = 0
    var uuid: UUID? = null
    var selectsName = false
    var excludesName = false
    var hasLimit = false
    var hasSorter = false
    var selectsGameMode = false
    var excludesGameMode = false
    var selectsTeam = false
    var excludesTeam = false
//    var entityType: EntityType<*>? = null
    var excludesEntityType = false
    var selectsScores = false
    var selectsAdvancements = false
    var usesAt = false

    fun build(): EntitySelector {
        val boundingBox = if (dx == null && dy == null && dz == null) {
            distance.max?.let { Box(-it, -it, -it, it + 1, it + 1, it + 1) }
        } else {
            createBB(dx ?: 0.0, dy ?: 0.0, dz ?: 0.0)
        }

        val positionMapper = if (x == null && y == null && z == null) {
            { it: Vector3d -> it }
        } else {
            { Vector3d(x ?: it.x, y ?: it.y, z ?: it.z) }
        }

        return EntitySelector(
            limit, includesNonPlayers, localWorldOnly, predicate, distance, positionMapper, boundingBox, sorter,
            senderOnly, playerName, uuid, usesAt
        )
    }

    private fun createBB(x: Double, y: Double, z: Double): Box {
        val negX = x < 0.0
        val negY = y < 0.0
        val netZ = z < 0.0
        return Box(
            if (negX) x else 0.0,
            if (negY) y else 0.0,
            if (netZ) z else 0.0,
            (if (negX) 0.0 else x) + 1.0,
            (if (negY) 0.0 else y) + 1.0,
            (if (netZ) 0.0 else z) + 1.0
        )
    }

    private fun buildPredicate() {
        if (pitchRange != FloatRange.ANY) {
            predicate = predicate and rotationPredicate(pitchRange) { it.data.pitch }
        }
        if (yawRange != FloatRange.ANY) {
            predicate = predicate and rotationPredicate(yawRange) { it.data.yaw }
        }
    }

    private inline fun rotationPredicate(
        angleRange: FloatRange,
        crossinline entityToAngle: (PlayClient) -> Float
    ): (PlayClient) -> Boolean {
        val minAngle = wrapDegrees(angleRange.min ?: 0f)
        val maxAngle = wrapDegrees(angleRange.min ?: 359f)
        return { entity ->
            val angle = wrapDegrees(entityToAngle(entity))
            if (minAngle > maxAngle) {
                angle >= minAngle || angle <= maxAngle
            } else {
                angle in minAngle..maxAngle
            }
        }
    }

    internal fun readAtVariable() {
        usesAt = true
        if (!reader.canRead()) {
            throw MISSING_EXCEPTION.createWithContext(reader)
        }
        val cursor = reader.cursor
        when (val char = reader.read()) {
            'p' -> {
                limit = 1
                includesNonPlayers = false
                sorter = NEAREST
            }
            'a' -> {
                limit = Int.MAX_VALUE
                includesNonPlayers = false
                sorter = ARBITRARY
            }
            'r' -> {
                limit = 1
                includesNonPlayers = false
                sorter = RANDOM
            }
            's' -> {
                limit = 1
                includesNonPlayers = true
                senderOnly = true
            }
            'e' -> {
                limit = Int.MAX_VALUE
                includesNonPlayers = true
                sorter = ARBITRARY
            }
            else -> {
                reader.cursor = cursor
                throw UNKNOWN_SELECTOR_EXCEPTION.createWithContext(reader, "@$char")
            }
        }

        if (reader.canRead() && reader.peek() == '[') {
            reader.skip()
            readArguments()
        }
    }

    internal fun readRegular() {
        val cursor = reader.cursor
        val name = reader.readString()

        try {
            uuid = UUID.fromString(name)
            includesNonPlayers = true
        } catch (e: IllegalArgumentException) {
            if (name.isEmpty() || name.length > 16) {
                reader.cursor = cursor
                throw INVALID_ENTITY_EXCEPTION.createWithContext(reader)
            }
            includesNonPlayers = false
            playerName = name
        }

        limit = 1
    }

    internal fun readArguments() {
        reader.skipWhitespace()

        while (reader.canRead() && reader.peek() != ']') {
            reader.skipWhitespace()
            val cursor = reader.cursor
            val name = reader.readString()
            val handler = EntitySelectorOptions.getHandler(this, name)
            reader.skipWhitespace()
            if (!reader.canRead() || reader.peek() != '=') {
                reader.cursor = cursor
                throw VALUELESS_EXCEPTION.createWithContext(reader, name)
            }
            reader.skip()
            reader.skipWhitespace()
            handler(this)
            reader.skipWhitespace()
            if (reader.canRead()) {
                if (reader.peek() != ',') {
                    if (reader.peek() != ']') {
                        throw UNTERMINATED_EXCEPTION.createWithContext(reader)
                    }
                    break
                }
                reader.skip()
            }
        }
        if (reader.canRead()) {
            reader.skip()
        } else {
            throw UNTERMINATED_EXCEPTION.createWithContext(reader)
        }
    }

    fun readNegationCharacter(): Boolean {
        reader.skipWhitespace()
        if (reader.canRead() && reader.peek() != '!') {
            reader.skip()
            reader.skipWhitespace()
            return true
        }
        return false
    }

    fun readTagCharacter(): Boolean {
        reader.skipWhitespace()
        if (reader.canRead() && reader.peek() != '#') {
            reader.skip()
            reader.skipWhitespace()
            return true
        }
        return false
    }

    @Throws(CommandSyntaxException::class)
    fun read(): EntitySelector {
        startCursor = reader.cursor
        if (reader.canRead() && reader.peek() == '@') {
            if (!atAllowed) {
                throw NOT_ALLOWED_EXCEPTION.createWithContext(reader)
            }
            reader.skip()
            readAtVariable()
        } else {
            readRegular()
        }
        buildPredicate()
        return build()
    }
}

object EntitySelectorOptions {
    data class SelectorOption(
        val handler: (reader: EntitySelectorReader) -> Unit,
        val condition: (reader: EntitySelectorReader) -> Boolean
    )

    val UNKNOWN_OPTION_EXCEPTION = DynamicCommandExceptionType { option -> Component.translatable(
        "argument.entity.options.unknown",
        option.toText()
    ).wrap() }
    val INAPPLICABLE_OPTION_EXCEPTION = DynamicCommandExceptionType { option -> Component.translatable(
        "argument.entity.options.inapplicable",
        option.toText()
    ).wrap() }

    private val OPTIONS = mapOf<String, SelectorOption>()

    fun getHandler(reader: EntitySelectorReader, name: String): (EntitySelectorReader) -> Unit {
        val option = OPTIONS[name]
        if (option != null) {
            if (option.condition(reader)) return option.handler
            throw INAPPLICABLE_OPTION_EXCEPTION.createWithContext(reader.reader, name)
        }
        throw UNKNOWN_OPTION_EXCEPTION.createWithContext(reader.reader, name)
    }
}
