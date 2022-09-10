package io.github.gaming32.mckt.packet.play.c2s

import io.github.gaming32.mckt.objects.ItemStack
import io.github.gaming32.mckt.packet.MinecraftInputStream
import io.github.gaming32.mckt.packet.MinecraftOutputStream
import io.github.gaming32.mckt.packet.Packet

class SetCreativeInventorySlotPacket(val slot: Int, val item: ItemStack?) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x2B
    }

    constructor(inp: MinecraftInputStream) : this(
        inp.readUnsignedShort(),
        inp.readItemStack()
    )

    override fun write(out: MinecraftOutputStream) {
        out.writeShort(slot)
        out.writeItemStack(item)
    }
}
