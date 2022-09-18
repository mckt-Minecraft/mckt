package io.github.gaming32.mckt.packet.play.c2s

import io.github.gaming32.mckt.data.MinecraftOutputStream
import io.github.gaming32.mckt.data.readItemStack
import io.github.gaming32.mckt.data.readUShort
import io.github.gaming32.mckt.objects.ItemStack
import io.github.gaming32.mckt.packet.Packet
import java.io.InputStream

data class SetCreativeInventorySlotPacket(val slot: Int, val item: ItemStack?) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x2B
    }

    constructor(inp: InputStream) : this(
        inp.readUShort().toInt(),
        inp.readItemStack()
    )

    override fun write(out: MinecraftOutputStream) {
        out.writeShort(slot)
        out.writeItemStack(item)
    }
}
