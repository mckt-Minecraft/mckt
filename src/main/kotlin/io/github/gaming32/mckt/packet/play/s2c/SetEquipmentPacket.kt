package io.github.gaming32.mckt.packet.play.s2c

import io.github.gaming32.mckt.data.writeByte
import io.github.gaming32.mckt.data.writeItemStack
import io.github.gaming32.mckt.data.writeVarInt
import io.github.gaming32.mckt.enumMapOf
import io.github.gaming32.mckt.objects.ItemStack
import io.github.gaming32.mckt.packet.Packet
import io.github.gaming32.mckt.toTypedArray
import java.io.OutputStream

class SetEquipmentPacket(val entityId: Int, vararg val equipment: Pair<Slot, ItemStack?>) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x53
    }

    enum class Slot(val rawSlot: Int) {
        MAIN_HAND(-1),
        OFFHAND(45),
        BOOTS(8),
        LEGGINGS(7),
        CHESTPLATE(6),
        HELMET(5);

        companion object {
            fun getSlot(rawSlot: Int): Slot? {
                if (rawSlot in 36..44) return MAIN_HAND
                return when (rawSlot) {
                    45 -> OFFHAND
                    8 -> BOOTS
                    7 -> LEGGINGS
                    6 -> CHESTPLATE
                    5 -> HELMET
                    else -> null
                }
            }
        }
    }

    init {
        require(equipment.isNotEmpty())
    }

    constructor(entityId: Int, equipment: Map<Slot, ItemStack?>) : this(entityId, *equipment.toTypedArray())

    val equipmentAsMap: Map<Slot, ItemStack?>
        get() = if (equipment.isEmpty()) mapOf() else enumMapOf(*equipment)

    override fun write(out: OutputStream) {
        out.writeVarInt(entityId)
        val iterator = equipment.iterator()
        do {
            val (slot, item) = iterator.next()
            val slotId = if (iterator.hasNext()) {
                slot.ordinal or 0x80
            } else {
                slot.ordinal
            }
            out.writeByte(slotId)
            out.writeItemStack(item)
        } while (iterator.hasNext())
    }

    override fun toString() = "SetEquipmentPacket(entityId=$entityId, equipment=$equipmentAsMap)"
}
