package io.github.gaming32.mckt.blocks.entities

import io.github.gaming32.mckt.getLogger
import io.github.gaming32.mckt.nbt.NbtCompound
import io.github.gaming32.mckt.objects.BlockPosition
import io.github.gaming32.mckt.objects.BlockState
import io.github.gaming32.mckt.objects.Identifier
import io.michaelrocks.bimap.HashBiMap
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream

object BlockEntities {
    private val LOGGER = getLogger()
    private val TYPES = HashBiMap<Identifier, BlockEntity.BlockEntityType<*>>()

    @Serializable
    data class BlockEntityTypeMetadata(val blocks: List<Identifier>, val networkId: Int)

    @OptIn(ExperimentalSerializationApi::class)
    private val METADATA = BlockEntities::class.java.getResourceAsStream("/dataexport/blockEntityTypes.json")?.use {
        Json.decodeFromStream<Map<Identifier, BlockEntityTypeMetadata>>(it)
    } ?: mapOf()

    fun register(id: Identifier, type: BlockEntity.BlockEntityType<*>) {
        TYPES[id] = type
    }

    fun getType(id: Identifier) = TYPES[id]

    fun BlockEntity.BlockEntityType<*>.getId() = TYPES.inverse[this]

    fun BlockEntity.BlockEntityType<*>.getMetadata() = METADATA[getId()]

    fun createFromNbt(pos: BlockPosition, state: BlockState, nbt: NbtCompound): BlockEntity<*>? {
        val id = Identifier.parse(nbt.getString("id"))
        return getType(id)
            ?.create(pos, state)
            ?.also { it.readNbt(nbt) }
            .also { if (it == null) {
                LOGGER.warn("Unknown block entity type $id")
            } }
    }

    fun blockPositionFromNbt(nbt: NbtCompound) = BlockPosition(nbt.getInt("x"), nbt.getInt("y"), nbt.getInt("z"))

    init {
        register(Identifier("sign"), SignBlockEntity)
    }
}
