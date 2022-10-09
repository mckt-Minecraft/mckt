package io.github.gaming32.mckt.objects

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import io.github.gaming32.mckt.*
import io.github.gaming32.mckt.GlobalPalette.BLOCKSTATE_TO_ID
import io.github.gaming32.mckt.GlobalPalette.BLOCK_STATE_PROPERTIES
import io.github.gaming32.mckt.GlobalPalette.DEFAULT_BLOCKSTATES
import io.github.gaming32.mckt.GlobalPalette.ID_TO_BLOCKSTATE
import io.github.gaming32.mckt.blocks.BlockEntityProvider
import io.github.gaming32.mckt.commands.wrap
import io.github.gaming32.mckt.items.BlockItemHandler
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.kyori.adventure.text.Component
import kotlin.collections.component1
import kotlin.collections.component2

@Serializable
class BlockState internal constructor(
    val blockId: Identifier = Identifier.EMPTY,
    @SerialName("id")
    val globalId: Int = -1,
    val properties: Map<String, String> = mapOf(),
    @SerialName("default")
    val defaultForId: Boolean = false
) {
    companion object {
        private val TAG_DISALLOWED = SimpleCommandExceptionType(
            Component.translatable("argument.block.tag.disallowed").wrap()
        )
        private val PROPERTY_UNCLOSED = SimpleCommandExceptionType(
            Component.translatable("argument.block.property.unclosed").wrap()
        )
        private val ID_INVALID = DynamicCommandExceptionType { blockName ->
            Component.translatable(
                "argument.block.id.invalid",
                blockName.toText()
            ).wrap()
        }
        private val PROPERTY_UNKNOWN = Dynamic2CommandExceptionType { blockName, propertyName ->
            Component.translatable(
                "argument.block.property.unknown",
                blockName.toText(), propertyName.toText()
            ).wrap()
        }
        private val PROPERTY_DUPLICATE = Dynamic2CommandExceptionType { blockName, propertyName ->
            Component.translatable(
                "argument.block.property.duplicate",
                propertyName.toText(), blockName.toText()
            ).wrap()
        }
        private val PROPERTY_INVALID = Dynamic3CommandExceptionType { blockName, propertyName, propertyValue ->
            Component.translatable(
                "argument.block.property.novalue",
                blockName.toText(), propertyName.toText(), propertyValue.toText()
            ).wrap()
        }
        private val PROPERTY_NOVALUE = Dynamic2CommandExceptionType { blockName, propertyName ->
            Component.translatable(
                "argument.block.property.novalue",
                propertyName.toText(), blockName.toText()
            ).wrap()
        }

        fun fromMap(map: Map<String, String>) = BlockState(
            blockId = Identifier.parse(
                map["blockId"] ?: throw IllegalArgumentException(
                    "Missing blockId field from block state data"
                )
            ),
            properties = map.toMutableMap().apply { remove("blockId") }
        ).canonicalize()

        fun parse(state: String): BlockState {
            val bracketIndex = state.indexOf('[')
            val blockId = Identifier.parse(
                if (bracketIndex == -1) {
                    state
                } else {
                    state.substring(0, bracketIndex)
                }
            )
            val properties = if (bracketIndex != -1) {
                if (state[state.length - 1] != ']') {
                    throw IllegalArgumentException("Mismatched [")
                }
                state.substring(bracketIndex + 1, state.length - 1).split(",").associate { prop ->
                    val info = prop.split("=", limit = 2)
                    if (info.size < 2) {
                        throw IllegalArgumentException("Missing \"=\": $prop")
                    }
                    info[0] to info[1]
                }
            } else {
                mapOf()
            }
            return DEFAULT_BLOCKSTATES[blockId]?.with(properties)?.canonicalize()
                ?: throw IllegalArgumentException("Unknown block ID $blockId")
        }

        fun parse(reader: StringReader): BlockState {
            if (reader.peek() == '#') {
                throw TAG_DISALLOWED.createWithContext(reader)
            }
            val cursor = reader.cursor
            val blockId = Identifier.parse(reader)
            val allowedProperties = BLOCK_STATE_PROPERTIES[blockId]
            if (allowedProperties == null) {
                reader.cursor = cursor
                throw ID_INVALID.createWithContext(reader, blockId)
            }
            val properties = mutableMapOf<String, String>()
            if (reader.canRead() && reader.peek() == '[') {
                reader.skip()
                reader.skipWhitespace()
                while (reader.canRead() && reader.peek() != ']') {
                    val name = reader.readUnquotedString()
                    val allowedValues = allowedProperties[name]
                    if (allowedValues == null) {
                        reader.cursor = cursor
                        throw PROPERTY_UNKNOWN.createWithContext(reader, blockId, name)
                    }
                    if (name in properties) {
                        reader.cursor = cursor
                        throw PROPERTY_DUPLICATE.createWithContext(reader, blockId, name)
                    }
                    reader.skipWhitespace()
                    if (!reader.canRead() || reader.peek() != '=') {
                        reader.cursor = cursor
                        throw PROPERTY_NOVALUE.createWithContext(reader, blockId, name)
                    }
                    reader.skip()
                    reader.skipWhitespace()
                    val value = reader.readUnquotedString()
                    if (value !in allowedValues) {
                        reader.cursor = cursor
                        throw PROPERTY_INVALID.createWithContext(reader, blockId, name, value)
                    }
                    properties[name] = value
                    reader.skipWhitespace()
                    if (reader.canRead() && reader.peek() == ',') {
                        reader.skip()
                    }
                    reader.skipWhitespace()
                }
                if (!reader.canRead()) {
                    reader.cursor = cursor
                    throw PROPERTY_UNCLOSED.createWithContext(reader)
                }
                reader.skip()
            }
            return DEFAULT_BLOCKSTATES[blockId]!!.with(properties)
        }
    }

    object CanonicalSerializer : KSerializer<BlockState> {
        override val descriptor = PrimitiveSerialDescriptor("BlockState", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: BlockState) = encoder.encodeString(value.toString())

        override fun deserialize(decoder: Decoder) = parse(decoder.decodeString())
    }

    @Transient
    var canonical = false
        internal set

    @Transient
    private val hash = blockId.hashCode() * 31 + properties.hashCode()

    val propertyOptions by lazy { BLOCK_STATE_PROPERTIES[blockId]!! }

    val blockProperties by lazy { BLOCK_PROPERTIES[blockId]!! }

    fun toMap() = properties + ("blockId" to blockId.toString())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BlockState

        // Canonical means interned. If this is interned, and it's not the same object as the other interned state,
        // it's not the same state.
        if (canonical && other.canonical) return false

        if (blockId != other.blockId) return false
        if (properties != other.properties) return false

        return true
    }

    override fun hashCode() = hash

    override fun toString() = buildString {
        append(blockId)
        if (properties.isNotEmpty()) {
            append('[')
            properties.entries.forEachIndexed { index, (name, value) ->
                if (index > 0) {
                    append(',')
                }
                append(name)
                append('=')
                append(value)
            }
            append(']')
        }
    }

    fun canonicalize() =
        if (canonical) {
            this
        } else {
            ID_TO_BLOCKSTATE.getOrNull(BLOCKSTATE_TO_ID.getInt(this))
                ?: throw IllegalArgumentException("Unknown block state: $this")
        }

    fun with(key: String, value: String) =
        BlockState(blockId, properties = properties.toMutableMap().apply { put(key, value) }).canonicalize()

    fun with(properties: Map<String, String>) =
        if (properties.isEmpty()) {
            this
        } else {
            BlockState(blockId, properties = this.properties + properties).canonicalize()
        }

    operator fun get(property: String) =
        properties[property]
            ?: throw IllegalArgumentException("Unknown property $property for block $blockId")

    fun cycle(property: String): BlockState {
        val value = properties[property]
        val options = propertyOptions[property]
            ?: throw IllegalArgumentException("Unknown property $property for block $blockId")
        val index = options.indexOf(value)
        return with(property, options[(index + 1) % options.size])
    }

    fun getHandler(server: MinecraftServer) = server.getBlockHandler(blockId)

    suspend fun onUse(world: World, client: PlayClient, hand: Hand, hit: BlockHitResult) =
        getHandler(world.server).onUse(this, world, hit.location, client, hand, hit)

    fun canReplace(ctx: BlockItemHandler.ItemPlacementContext) =
        getHandler(ctx.server).canReplace(this, ctx)

    suspend fun getStateForNeighborUpdate(
        direction: Direction,
        neighborState: BlockState,
        world: World,
        pos: BlockPosition,
        neighborPos: BlockPosition
    ) = getHandler(world.server).getStateForNeighborUpdate(this, direction, neighborState, world, pos, neighborPos)

    suspend fun canPlaceAt(world: World, pos: BlockPosition) = getHandler(world.server).canPlaceAt(this, world, pos)

    suspend fun prepare(
        world: World,
        pos: BlockPosition,
        flags: Int,
        maxUpdateDepth: Int = 512
    ) = getHandler(world.server).prepare(this, world, pos, flags, maxUpdateDepth)

    suspend fun neighborUpdate(
        world: World,
        pos: BlockPosition,
        block: Identifier,
        fromPos: BlockPosition,
        notify: Boolean
    ) = getHandler(world.server).neighborUpdate(this, world, pos, block, fromPos, notify)

    fun onStateReplaced(
        world: World,
        pos: BlockPosition,
        newState: BlockState,
        moved: Boolean
    ) = getHandler(world.server).onStateReplaced(this, world, pos, newState, moved)

    fun hasBlockEntity(server: MinecraftServer) = getHandler(server) is BlockEntityProvider<*>
}
