package io.github.gaming32.mckt.objects

import io.github.gaming32.mckt.Blocks
import io.github.gaming32.mckt.MinecraftServer
import io.github.gaming32.mckt.PlayClient
import io.github.gaming32.mckt.World
import io.github.gaming32.mckt.dt.DtCompound
import io.github.gaming32.mckt.dt.NbtTagType
import io.github.gaming32.mckt.items.ItemHandler
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class ItemStack(
    val itemId: Identifier?,
    var count: Int = 1,
    @Serializable(DtCompoundSerializer::class)
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("extraNbt")
    private var extraNbtInternal: DtCompound? = null
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

    fun getOrCreateSubNbt(key: String): DtCompound {
        val nbt = extraNbtInternal
        if (nbt != null && nbt.contains(key, NbtTagType.COMPOUND)) {
            return nbt.getCompound(key)
        }
        val result = DtCompound()
        this[key] = result
        return result
    }

    operator fun get(key: String) =
        if (extraNbtInternal?.contains(key, NbtTagType.COMPOUND) == true) {
            extraNbtInternal?.getCompound(key)
        } else {
            null
        }

    operator fun set(key: String, nbt: DtCompound) {
        getOrCreateNbt()[key] = nbt
    }

    fun getOrCreateNbt(): DtCompound {
        var result = extraNbtInternal
        if (result == null) {
            result = DtCompound()
            extraNbtInternal = result
        }
        return result
    }
}

fun ItemStack?.orEmpty() = this ?: ItemStack.EMPTY
