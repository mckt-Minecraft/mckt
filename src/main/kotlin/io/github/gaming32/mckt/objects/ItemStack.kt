package io.github.gaming32.mckt.objects

import kotlinx.serialization.Serializable
import net.benwoodworth.knbt.NbtCompound

@Serializable
data class ItemStack(
    val itemId: Identifier,
    var count: Int,
    @Serializable(SNbtCompoundSerializer::class)
    val extraNbt: NbtCompound?
)
