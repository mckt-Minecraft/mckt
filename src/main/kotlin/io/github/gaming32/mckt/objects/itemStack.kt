package io.github.gaming32.mckt.objects

import io.github.gaming32.mckt.Blocks
import io.github.gaming32.mckt.MinecraftServer
import io.github.gaming32.mckt.PlayClient
import io.github.gaming32.mckt.World
import io.github.gaming32.mckt.items.ItemHandler
import io.github.gaming32.mckt.nbt.NbtCompound
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.kyori.adventure.nbt.BinaryTagTypes

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class ItemStack(
    val itemId: Identifier?,
    var count: Int = 1,
    @Serializable(SnbtCompoundSerializer::class)
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("extraNbt")
    private var extraNbtInternal: NbtCompound? = null
) {
    companion object {
        val EMPTY = ItemStack(null)
    }

    val extraNbt get() = extraNbtInternal

    fun isEmpty() =
        if (itemId == null || itemId == Blocks.AIR.blockId) {
            true
        } else {
            count <= 0
        }

    @Suppress("NOTHING_TO_INLINE")
    inline fun isNotEmpty() = !isEmpty()

    fun copy() =
        if (isEmpty()) {
            EMPTY
        } else {
            val stack = ItemStack(itemId, count)
            extraNbtInternal?.let { stack.extraNbtInternal = it.deepCopy() }
            stack
        }

    fun decrement() {
        if (--count < 0) count = 0
    }

    fun decrement(amount: Int) {
        count -= amount
        if (count < 0) count = 0
    }

    fun getHandler(server: MinecraftServer) = server.getItemHandler(itemId)

    suspend fun useOnBlock(ctx: ItemHandler.ItemUsageContext) =
        getHandler(ctx.client.server).useOnBlock(ctx)

    suspend fun postMine(
        world: World,
        state: BlockState,
        location: BlockPosition,
        miner: PlayClient
    ) {
        getHandler(miner.server).postMine(this, world, state, location, miner)
    }

    suspend fun use(world: World, client: PlayClient, hand: Hand) =
        getHandler(client.server).use(world, client, hand)

    fun getOrCreateSubNbt(key: String): NbtCompound {
        val nbt = extraNbtInternal
        if (nbt != null && nbt.contains(key, BinaryTagTypes.COMPOUND)) {
            return nbt.getCompound(key)
        }
        return NbtCompound().also { this[key] = it }
    }

    operator fun get(key: String) =
        if (extraNbtInternal?.contains(key, BinaryTagTypes.COMPOUND) == true) {
            extraNbtInternal?.getCompound(key)
        } else {
            null
        }

    operator fun set(key: String, nbt: NbtCompound) {
        getOrCreateNbt()[key] = nbt
    }

    fun getOrCreateNbt(): NbtCompound {
        var result = extraNbtInternal
        if (result == null) {
            result = NbtCompound()
            extraNbtInternal = result
        }
        return result
    }
}

fun ItemStack?.orEmpty() = this ?: ItemStack.EMPTY
