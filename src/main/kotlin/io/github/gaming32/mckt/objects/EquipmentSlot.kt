package io.github.gaming32.mckt.objects

enum class EquipmentSlot(val rawSlot: Int) {
    MAIN_HAND(-1),
    OFFHAND(45),
    BOOTS(8),
    LEGGINGS(7),
    CHESTPLATE(6),
    HELMET(5);

    companion object {
        fun getSlot(rawSlot: Int): EquipmentSlot? {
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
