package io.github.gaming32.mckt.packet.play.s2c

import io.github.gaming32.mckt.blocks.entities.BlockEntities.getId
import io.github.gaming32.mckt.blocks.entities.BlockEntities.getMetadata
import io.github.gaming32.mckt.blocks.entities.BlockEntity
import io.github.gaming32.mckt.data.writeBlockPosition
import io.github.gaming32.mckt.data.writeNullableNbt
import io.github.gaming32.mckt.data.writeVarInt
import io.github.gaming32.mckt.nbt.NbtCompound
import io.github.gaming32.mckt.objects.BlockPosition
import io.github.gaming32.mckt.packet.Packet
import java.io.OutputStream

data class BlockEntityDataPacket(
    val pos: BlockPosition,
    val entityType: BlockEntity.BlockEntityType<*>,
    val data: NbtCompound?
) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x07
    }

    override fun write(out: OutputStream) {
        out.writeBlockPosition(pos)
        out.writeVarInt(entityType.getMetadata()?.networkId
            ?: throw IllegalArgumentException("Cannot sync block entity ${entityType.getId()} to the network.")
        )
        out.writeNullableNbt(data)
    }
}

fun <T : BlockEntity<T>> BlockEntityDataPacket(
    entity: T,
    serializer: (T) -> NbtCompound = BlockEntity<T>::initialNetworkSerialize
) = BlockEntityDataPacket(entity.pos, entity.type, serializer(entity))
