package io.github.gaming32.mckt.packet.play.c2s

import io.github.gaming32.mckt.data.readItemStack
import io.github.gaming32.mckt.data.readUShort
import io.github.gaming32.mckt.data.writeItemStack
import io.github.gaming32.mckt.data.writeShort
import io.github.gaming32.mckt.objects.ItemStack
import io.github.gaming32.mckt.packet.Packet
import java.io.InputStream
import java.io.OutputStream

data class SetCreativeInventorySlotPacket(val slot: Int, val item: ItemStack) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x2B
    }

    constructor(inp: InputStream) : this(
        inp.readUShort().toInt(),
        inp.readItemStack()
    )

    override fun write(out: OutputStream) {
        out.writeShort(slot)
        out.writeItemStack(item)
    }
}
