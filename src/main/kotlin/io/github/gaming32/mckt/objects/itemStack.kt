package io.github.gaming32.mckt.objects

import io.github.gaming32.mckt.Blocks
import io.github.gaming32.mckt.MinecraftServer
import io.github.gaming32.mckt.PlayClient
import io.github.gaming32.mckt.World
import io.github.gaming32.mckt.data.deepCopy
import io.github.gaming32.mckt.items.ItemHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.benwoodworth.knbt.NbtCompound

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class ItemStack(
    val itemId: Identifier?,
    var count: Int = 1,
    @Serializable(SNbtCompoundSerializer::class)
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

    fun getHandler(server: MinecraftServer) = server.getItemHandler(itemId)

    suspend fun useOnBlock(ctx: ItemHandler.ItemUsageContext, scope: CoroutineScope) =
        getHandler(ctx.client.server).useOnBlock(ctx, scope)

    suspend fun postMine(
        world: World,
        state: BlockState,
        location: BlockPosition,
        miner: PlayClient,
        scope: CoroutineScope
    ) {
        getHandler(miner.server).postMine(this, world, state, location, miner, scope)
    }

    suspend fun use(world: World, client: PlayClient, hand: Hand, scope: CoroutineScope) =
        getHandler(client.server).use(world, client, hand, scope)
}

fun ItemStack?.orEmpty() = this ?: ItemStack.EMPTY
