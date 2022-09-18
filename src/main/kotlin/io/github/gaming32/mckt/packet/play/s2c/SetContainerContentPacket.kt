package io.github.gaming32.mckt.packet.play.s2c

import io.github.gaming32.mckt.data.writeByte
import io.github.gaming32.mckt.data.writeItemStack
import io.github.gaming32.mckt.data.writeItemStackArray
import io.github.gaming32.mckt.data.writeVarInt
import io.github.gaming32.mckt.objects.ItemStack
import io.github.gaming32.mckt.packet.Packet
import java.io.OutputStream

class SetContainerContentPacket(
    val windowId: UByte,
    vararg val slots: ItemStack?,
    val carriedItem: ItemStack? = null
) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x11
    }

    override fun write(out: OutputStream) {
        out.writeByte(windowId.toInt())
        out.writeVarInt(0) // State ID
        out.writeItemStackArray(slots)
        out.writeItemStack(carriedItem)
    }

    override fun toString() = "SetContainerContentPacket(windowId=$windowId, slots=..., carriedItem=$carriedItem)"
}
