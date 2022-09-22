package io.github.gaming32.mckt.objects

import io.github.gaming32.mckt.GlobalPalette.BLOCKSTATE_TO_ID
import io.github.gaming32.mckt.GlobalPalette.ID_TO_BLOCKSTATE
import io.github.gaming32.mckt.MinecraftServer
import io.github.gaming32.mckt.PlayClient
import io.github.gaming32.mckt.World
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.collections.Map
import kotlin.collections.associate
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.forEachIndexed
import kotlin.collections.getOrNull
import kotlin.collections.isNotEmpty
import kotlin.collections.mapOf
import kotlin.collections.plus
import kotlin.collections.toMutableMap

@Serializable
data class BlockState(
    val blockId: Identifier = Identifier.EMPTY,
    @SerialName("id")
    val globalId: Int = -1,
    val properties: Map<String, String> = mapOf(),
    @SerialName("default")
    val defaultForId: Boolean = false
) {
    companion object {
        fun fromMap(map: Map<String, String>) = BlockState(
            blockId = Identifier.parse(
                map["blockId"] ?: throw IllegalArgumentException(
                    "Missing blockId field from block state data"
                )
            ),
            properties = map.toMutableMap().apply { remove("blockId") }
        )

        fun parse(state: String): BlockState {
            val bracketIndex = state.indexOf('[')
            val blockId = Identifier.parse(
                if (bracketIndex == -1) {
                    state
                } else {
                    state.substring(0, bracketIndex)
                }
            )
            if (bracketIndex == -1) return BlockState(blockId)
            if (state[state.length - 1] != ']') {
                throw IllegalArgumentException("Mismatched [")
            }
            val properties = state.substring(bracketIndex + 1, state.length - 1).split(",").associate { prop ->
                val info = prop.split("=", limit = 2)
                if (info.size < 2) {
                    throw IllegalArgumentException("Missing \"=\": $prop")
                }
                info[0] to info[1]
            }
            return BlockState(blockId, properties = properties)
        }
    }

    fun toMap() = properties + ("blockId" to blockId.toString())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BlockState

        if (blockId != other.blockId) return false
        if (properties != other.properties) return false

        return true
    }

    override fun hashCode(): Int {
        var result = blockId.hashCode()
        result = 31 * result + properties.hashCode()
        return result
    }

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

    fun canonicalizeOrNull() = ID_TO_BLOCKSTATE.getOrNull(BLOCKSTATE_TO_ID.getInt(this))

    fun canonicalizeOrThis() = canonicalizeOrNull() ?: this

    fun canonicalize() = canonicalizeOrNull() ?: throw IllegalArgumentException("Unknown block state: $this")

    fun with(key: String, value: String) = BlockState(blockId, properties = properties + mapOf(key to value))

    fun with(properties: Map<String, String>) = BlockState(blockId, properties = properties)

    fun getHandler(server: MinecraftServer) = server.getBlockHandler(blockId)

    suspend fun onUse(world: World, client: PlayClient, hand: Hand, hit: BlockHitResult, scope: CoroutineScope) =
        getHandler(world.server).onUse(this, world, hit.location, client, hand, hit, scope)
}
