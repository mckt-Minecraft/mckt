package io.github.gaming32.mckt.world

import io.github.gaming32.mckt.Gamemode
import io.github.gaming32.mckt.enumMapOf
import io.github.gaming32.mckt.objects.EntityPose
import io.github.gaming32.mckt.objects.EquipmentSlot
import io.github.gaming32.mckt.objects.Hand
import io.github.gaming32.mckt.objects.ItemStack
import kotlinx.serialization.Serializable

@Serializable
class PlayerData(
    var x: Double = 0.0,
    var y: Double = 0.0,
    var z: Double = 0.0
) {
    var yaw = 0f
    var pitch = 0f
    var onGround = false
    var flying = false
    var operatorLevel = 0
    var gamemode = Gamemode.CREATIVE
    val inventory = Array(46) { ItemStack.EMPTY }
    var selectedHotbarSlot = 0
        set(value) {
            require(value in 0..9) { "Hotbar slot not in range 0..9" }
            field = value
        }

    var selectedInventorySlot
        get() = selectedHotbarSlot + 36
        set(value) {
            selectedHotbarSlot = value - 36
        }

    var mainHand
        get() = inventory[selectedInventorySlot]
        set(value) {
            inventory[selectedInventorySlot] = value
        }
    var offhand
        get() = inventory[45]
        set(value) {
            inventory[45] = value
        }

    fun getHeldItem(hand: Hand) = if (hand == Hand.MAINHAND) mainHand else offhand
    fun setHeldItem(hand: Hand, stack: ItemStack) {
        if (hand == Hand.MAINHAND) {
            mainHand = stack
        } else {
            offhand = stack
        }
    }

    var flags: Int = 0
    var pose: EntityPose = EntityPose.STANDING

    var isSneaking: Boolean
        get() = (flags and EntityFlags.SNEAKING) != 0
        set(value) {
            flags = if (value) {
                flags or EntityFlags.SNEAKING
            } else {
                flags and EntityFlags.SNEAKING.inv()
            }
        }

    var isSprinting: Boolean
        get() = (flags and EntityFlags.SPRINTING) != 0
        set(value) {
            flags = if (value) {
                flags or EntityFlags.SPRINTING
            } else {
                flags and EntityFlags.SPRINTING.inv()
            }
        }

    var isFallFlying: Boolean
        get() = (flags and EntityFlags.FALL_FLYING) != 0
        set(value) {
            flags = if (value) {
                flags or EntityFlags.FALL_FLYING
            } else {
                flags and EntityFlags.FALL_FLYING.inv()
            }
        }
    fun getEquipment(): Map<EquipmentSlot, ItemStack> {
        val result = enumMapOf<EquipmentSlot, ItemStack>()
        for (slot in EquipmentSlot.values()) {
            val rawSlot = if (slot.rawSlot == -1) selectedInventorySlot else slot.rawSlot
            if (inventory[rawSlot].isNotEmpty()) {
                result[slot] = inventory[rawSlot]
            }
        }
        return result
    }
}

object EntityFlags {
    const val BURNING = 0x01
    const val SNEAKING = 0x02
    const val SPRINTING = 0x08
    const val SWIMMING = 0x10
    const val INVISIBLE = 0x20
    const val GLOWING = 0x40
    const val FALL_FLYING = 0x80
}