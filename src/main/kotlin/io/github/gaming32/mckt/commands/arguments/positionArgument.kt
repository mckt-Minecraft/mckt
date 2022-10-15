package io.github.gaming32.mckt.commands.arguments

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import io.github.gaming32.mckt.commands.CommandSource
import io.github.gaming32.mckt.commands.wrap
import io.github.gaming32.mckt.objects.Vector2f
import io.github.gaming32.mckt.objects.Vector3d
import io.github.gaming32.mckt.world.isInBuildLimit
import io.github.gaming32.mckt.world.isValidForWorld
import net.kyori.adventure.text.Component
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin


val INCOMPLETE_POSITION_EXCEPTION = SimpleCommandExceptionType(
    Component.translatable("argument.pos3d.incomplete").wrap()
)
val MIXED_COORDINATE_EXCEPTION = SimpleCommandExceptionType(
    Component.translatable("argument.pos.mixed").wrap()
)

sealed interface PositionArgument {
    fun toAbsolutePosition(source: CommandSource): Vector3d

    fun toAbsoluteRotation(source: CommandSource): Vector2f

    fun toAbsoluteBlockPosition(source: CommandSource) = toAbsolutePosition(source).toBlockPosition()

    val isXRelative: Boolean
    val isYRelative: Boolean
    val isZRelative: Boolean
}

data class CoordinateArgument(val relative: Boolean, val value: Double) {
    companion object Parser {
        val MISSING_COORDINATE = SimpleCommandExceptionType(
            Component.translatable("argument.pos.missing.double").wrap()
        )
        val MISSING_BLOCK_POSITION = SimpleCommandExceptionType(
            Component.translatable("argument.pos.missing.int").wrap()
        )

        @Throws(CommandSyntaxException::class)
        fun parse(reader: StringReader, centerIntegers: Boolean) =
            if (reader.canRead() && reader.peek() == '^') {
                throw MIXED_COORDINATE_EXCEPTION.createWithContext(reader)
            } else if (!reader.canRead()) {
                throw MISSING_COORDINATE.createWithContext(reader)
            } else {
                val relative = isRelative(reader)
                val cursor = reader.cursor
                var value = if (reader.canRead() && reader.peek() != ' ') reader.readDouble() else 0.0
                val asString = reader.string.substring(cursor, reader.cursor)
                if (relative && asString.isEmpty()) {
                    CoordinateArgument(true, 0.0)
                } else {
                    if ('.' !in asString && !relative && centerIntegers) {
                        value += 0.5
                    }
                    CoordinateArgument(relative, value)
                }
            }

        @Throws(CommandSyntaxException::class)
        fun parse(reader: StringReader) =
            if (reader.canRead() && reader.peek() == '^') {
                throw MIXED_COORDINATE_EXCEPTION.createWithContext(reader)
            } else if (!reader.canRead()) {
                throw MISSING_COORDINATE.createWithContext(reader)
            } else {
                val relative = isRelative(reader)
                CoordinateArgument(
                    relative,
                    if (reader.canRead() && reader.peek() != ' ') {
                        if (relative) reader.readDouble() else reader.readInt().toDouble()
                    } else {
                        0.0
                    }
                )
            }

        fun isRelative(reader: StringReader) = if (reader.peek() == '~') {
            reader.skip()
            true
        } else false
    }

    fun toAbsoluteCoordinate(offset: Double) = if (relative) value + offset else value
}

data class DefaultPositionArgument(
    val x: CoordinateArgument,
    val y: CoordinateArgument,
    val z: CoordinateArgument
) : PositionArgument {
    companion object Parser {
        @Throws(CommandSyntaxException::class)
        fun parse(reader: StringReader): DefaultPositionArgument {
            val cursor = reader.cursor
            val xCoord = CoordinateArgument.parse(reader)
            if (reader.canRead() && reader.peek() == ' ') {
                reader.skip()
                val yCoord = CoordinateArgument.parse(reader)
                if (reader.canRead() && reader.peek() == ' ') {
                    reader.skip()
                    val zCoord = CoordinateArgument.parse(reader)
                    return DefaultPositionArgument(xCoord, yCoord, zCoord)
                }
            }
            reader.cursor = cursor
            throw INCOMPLETE_POSITION_EXCEPTION.createWithContext(reader)
        }

        @Throws(CommandSyntaxException::class)
        fun parse(reader: StringReader, centerIntegers: Boolean): DefaultPositionArgument {
            val cursor = reader.cursor
            val xCoord = CoordinateArgument.parse(reader, centerIntegers)
            if (reader.canRead() && reader.peek() == ' ') {
                reader.skip()
                val yCoord = CoordinateArgument.parse(reader, false)
                if (reader.canRead() && reader.peek() == ' ') {
                    reader.skip()
                    val zCoord = CoordinateArgument.parse(reader, centerIntegers)
                    return DefaultPositionArgument(xCoord, yCoord, zCoord)
                }
            }
            reader.cursor = cursor
            throw INCOMPLETE_POSITION_EXCEPTION.createWithContext(reader)
        }

        fun absolute(x: Double, y: Double, z: Double) = DefaultPositionArgument(
            CoordinateArgument(false, x),
            CoordinateArgument(false, y),
            CoordinateArgument(false, z)
        )

        fun zero() = DefaultPositionArgument(
            CoordinateArgument(true, 0.0),
            CoordinateArgument(true, 0.0),
            CoordinateArgument(true, 0.0)
        )
    }

    override fun toAbsolutePosition(source: CommandSource): Vector3d {
        val offset = source.position
        return Vector3d(
            x.toAbsoluteCoordinate(offset.x),
            y.toAbsoluteCoordinate(offset.y),
            z.toAbsoluteCoordinate(offset.z)
        )
    }

    override fun toAbsoluteRotation(source: CommandSource): Vector2f {
        val offset = source.rotation
        return Vector2f(
            x.toAbsoluteCoordinate(offset.x.toDouble()).toFloat(),
            y.toAbsoluteCoordinate(offset.y.toDouble()).toFloat()
        )
    }

    override val isXRelative get() = x.relative
    override val isYRelative get() = y.relative
    override val isZRelative get() = z.relative
}

data class LookingPositionArgument(val x: Double, val y: Double, val z: Double) : PositionArgument {
    companion object Parser {
        @Throws(CommandSyntaxException::class)
        fun parse(reader: StringReader): LookingPositionArgument {
            val cursor = reader.cursor
            val x = readCoordinate(reader, cursor)
            if (reader.canRead() && reader.peek() == ' ') {
                reader.skip()
                val y = readCoordinate(reader, cursor)
                if (reader.canRead() && reader.peek() == ' ') {
                    reader.skip()
                    val z = readCoordinate(reader, cursor)
                    return LookingPositionArgument(x, y, z)
                }
            }
            reader.cursor = cursor
            throw INCOMPLETE_POSITION_EXCEPTION.createWithContext(reader)
        }

        private fun readCoordinate(reader: StringReader, startingCursor: Int) =
            if (!reader.canRead()) {
                throw CoordinateArgument.MISSING_COORDINATE.createWithContext(reader)
            } else if (reader.peek() != '^') {
                reader.cursor = startingCursor
                throw MIXED_COORDINATE_EXCEPTION.createWithContext(reader)
            } else {
                reader.skip()
                if (reader.canRead() && reader.peek() != ' ') {
                    reader.readDouble()
                } else {
                    0.0
                }
            }
    }

    override fun toAbsolutePosition(source: CommandSource): Vector3d {
        // No clue how this works
        val rotation = source.rotation
        val f = cos((rotation.y + 90f) * (PI / 180.0).toFloat())
        val g = sin((rotation.y + 90.0f) * (Math.PI / 180.0).toFloat())
        val h = cos(-rotation.x * (Math.PI / 180.0).toFloat())
        val i = sin(-rotation.x * (Math.PI / 180.0).toFloat())
        val j = cos((-rotation.x + 90.0f) * (Math.PI / 180.0).toFloat())
        val k = sin((-rotation.x + 90.0f) * (Math.PI / 180.0).toFloat())
        val vec3d2 = Vector3d((f * h).toDouble(), i.toDouble(), (g * h).toDouble())
        val vec3d3 = Vector3d((f * j).toDouble(), k.toDouble(), (g * j).toDouble())
        val vec3d4 = (vec3d2 cross vec3d3) * -1.0
        val d = vec3d2.x * z + vec3d3.x * y + vec3d4.x * x
        val e = vec3d2.y * z + vec3d3.y * y + vec3d4.y * x
        val l = vec3d2.z * z + vec3d3.z * y + vec3d4.z * x
        val position = source.position
        return Vector3d(position.x + d, position.y + e, position.z + l)
    }

    override fun toAbsoluteRotation(source: CommandSource) = Vector2f.ZERO

    override val isXRelative get() = true
    override val isYRelative get() = true
    override val isZRelative get() = true
}

data class Vector3ArgumentType @JvmOverloads constructor(
    val centerIntegers: Boolean = true
) : ArgumentType<PositionArgument> {
    companion object {
        private val EXAMPLES = listOf("0 0 0", "~ ~ ~", "^ ^ ^", "^1 ^ ^-5", "0.1 -0.5 .9", "~0.5 ~1 ~-5")
    }

    override fun parse(reader: StringReader) =
        if (reader.canRead() && reader.peek() == '^') {
            LookingPositionArgument.parse(reader)
        } else {
            DefaultPositionArgument.parse(reader, centerIntegers)
        }

    override fun getExamples() = EXAMPLES
}

object BlockPositionArgumentType : ArgumentType<PositionArgument> {
    private val EXAMPLES = listOf("0 0 0", "~ ~ ~", "^ ^ ^", "^1 ^ ^-5", "~0.5 ~1 ~-5")
    val UNLOADED_EXCEPTION = SimpleCommandExceptionType(Component.translatable("argument.pos.unloaded").wrap())
    val OUT_OF_WORLD_EXCEPTION = SimpleCommandExceptionType(Component.translatable("argument.pos.outofworld").wrap())
    val OUT_OF_BOUNDS_EXCEPTION = SimpleCommandExceptionType(Component.translatable("argument.pos.outofbounds").wrap())

    override fun parse(reader: StringReader) =
        if (reader.canRead() && reader.peek() == '^') {
            LookingPositionArgument.parse(reader)
        } else {
            DefaultPositionArgument.parse(reader)
        }

    override fun getExamples() = EXAMPLES
}

fun CommandContext<CommandSource>.getVec3(name: String) = getPositionArgument(name).toAbsolutePosition(source)

fun CommandContext<CommandSource>.getPositionArgument(name: String) = getArgument(name, PositionArgument::class.java)!!

fun CommandContext<CommandSource>.getLoadedBlockPosition(name: String) =
    getArgument(name, PositionArgument::class.java).toAbsoluteBlockPosition(source).also { pos ->
        if (!source.server.world.isBlockLoaded(pos)) {
            throw BlockPositionArgumentType.UNLOADED_EXCEPTION.create()
        } else if (!pos.isInBuildLimit) {
            throw BlockPositionArgumentType.OUT_OF_WORLD_EXCEPTION.create()
        }
    }

fun CommandContext<CommandSource>.getBlockPosition(name: String) =
    getArgument(name, PositionArgument::class.java).toAbsoluteBlockPosition(source).also { pos ->
        if (!pos.isValidForWorld) {
            throw BlockPositionArgumentType.OUT_OF_BOUNDS_EXCEPTION.create()
        }
    }
