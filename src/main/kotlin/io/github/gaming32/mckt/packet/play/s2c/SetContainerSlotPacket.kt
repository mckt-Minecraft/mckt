package io.github.gaming32.mckt.packet.play.s2c

import io.github.gaming32.mckt.data.writeByte
import io.github.gaming32.mckt.data.writeItemStack
import io.github.gaming32.mckt.data.writeShort
import io.github.gaming32.mckt.data.writeVarInt
import io.github.gaming32.mckt.objects.ItemStack
import io.github.gaming32.mckt.packet.Packet
import java.io.OutputStream

data class SetContainerSlotPacket(val windowId: Byte, val slotId: Int, val item: ItemStack) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x13
    }

    override fun write(out: OutputStream) {
        out.writeByte(windowId.toInt())
        out.writeVarInt(0)
        out.writeShort(slotId)
        out.writeItemStack(item)
    }
}
