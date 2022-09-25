package io.github.gaming32.mckt

import io.github.gaming32.mckt.objects.BlockState
import io.github.gaming32.mckt.objects.Identifier
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*

// blockStates.json and registries.json were generated from the Vanilla server JAR with its builtin tool
object GlobalPalette {
    val BLOCK_STATE_PROPERTIES: Map<Identifier, Map<String, List<String>>>
    val GLOBAL_PALETTE: Set<BlockState>

    init {
        @OptIn(ExperimentalSerializationApi::class)
        val paletteData = MinecraftServer::class.java.getResourceAsStream("/blockStates.json")?.use {
            Json.decodeFromStream<JsonObject>(it)
        } ?: JsonObject(emptyMap())

        BLOCK_STATE_PROPERTIES = paletteData.asSequence().associate { (blockId, blockData) ->
            blockData as JsonObject
            Identifier.parse(blockId) to (blockData["properties"]
                .castOrNull<JsonObject>()
                ?.asSequence()
                ?.associate { (name, values) ->
                    name to values.cast<JsonArray>().map { it.cast<JsonPrimitive>().content }
                } ?: mapOf())
        }

        GLOBAL_PALETTE = paletteData.asSequence().flatMap { (blockId, data) ->
            data as JsonObject
            val blockIdAsIdentifier = Identifier.parse(blockId)
            (data["states"] as JsonArray)
                .asSequence()
                .map {
                    val state = Json.decodeFromJsonElement<BlockState>(it)
                    val new = BlockState(blockIdAsIdentifier, state.globalId, state.properties, state.defaultForId)
                    new.canonical = true
                    new
                }
        }.toSet()
    }

    val DEFAULT_BLOCKSTATES = GLOBAL_PALETTE
        .asSequence()
        .filter(BlockState::defaultForId)
        .associateBy(BlockState::blockId)

    @Suppress("UNCHECKED_CAST")
    val ID_TO_BLOCKSTATE = arrayOfNulls<BlockState>(GLOBAL_PALETTE.size).apply {
        GLOBAL_PALETTE.forEach { this[it.globalId] = it }
    } as Array<BlockState>

    val BLOCKSTATE_TO_ID = GLOBAL_PALETTE.associateWithTo(Object2IntOpenHashMap()) {
        it.globalId
    }.apply { defaultReturnValue(-1) }
}
