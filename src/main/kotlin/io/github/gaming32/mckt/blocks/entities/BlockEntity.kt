package io.github.gaming32.mckt.blocks.entities

import io.github.gaming32.mckt.World
import io.github.gaming32.mckt.blocks.entities.BlockEntities.getId
import io.github.gaming32.mckt.nbt.NbtCompound
import io.github.gaming32.mckt.objects.BlockPosition
import io.github.gaming32.mckt.objects.BlockState
import io.github.gaming32.mckt.packet.Packet

abstract class BlockEntity<T : BlockEntity<T>>(
    val type: BlockEntityType<T>,
    val pos: BlockPosition,
    var cachedState: BlockState
) {
    abstract class BlockEntityType<T : BlockEntity<T>> {
        abstract fun create(pos: BlockPosition, state: BlockState): T
    }

    var world: World? = null

    open fun readNbt(nbt: NbtCompound) = Unit

    open fun writeNbt(nbt: NbtCompound) = Unit

    open fun initialNetworkSerialize() = NbtCompound()

    open fun updateNetworkSerialize(): Packet? = null

    fun toNbt(): NbtCompound {
        val nbt = NbtCompound()
        writeNbt(nbt)
        return nbt
    }

    fun toIdentifiedNbt(): NbtCompound {
        val nbt = toNbt()
        nbt.putString("id", type.getId()?.toString()
            ?: throw IllegalStateException("${javaClass.simpleName} does not have an ID")
        )
        return nbt
    }

    fun toIdentifiedLocatedNbt(): NbtCompound {
        val nbt = toIdentifiedNbt()
        nbt.putInt("x", pos.x)
        nbt.putInt("y", pos.y)
        nbt.putInt("z", pos.z)
        return nbt
    }
}
