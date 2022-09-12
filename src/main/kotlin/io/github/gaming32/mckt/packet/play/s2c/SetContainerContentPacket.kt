package io.github.gaming32.mckt.packet.play.s2c

import io.github.gaming32.mckt.objects.ItemStack
import io.github.gaming32.mckt.packet.MinecraftOutputStream
import io.github.gaming32.mckt.packet.Packet

class SetContainerContentPacket(
    val windowId: UByte,
    vararg val slots: ItemStack?,
    val carriedItem: ItemStack? = null
) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x11
    }

    override fun write(out: MinecraftOutputStream) {
        out.writeByte(windowId.toInt())
        out.writeVarInt(0) // State ID
        out.writeVarInt(slots.size)
        slots.forEach { out.writeItemStack(it) }
        out.writeItemStack(carriedItem)
    }

    override fun toString() = "SetContainerContentPacket(windowId=$windowId, slots=..., carriedItem=$carriedItem)"
}
