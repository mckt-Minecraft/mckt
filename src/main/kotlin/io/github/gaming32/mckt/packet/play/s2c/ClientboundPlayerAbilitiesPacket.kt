package io.github.gaming32.mckt.packet.play.s2c

import io.github.gaming32.mckt.packet.MinecraftOutputStream
import io.github.gaming32.mckt.packet.Packet

class ClientboundPlayerAbilitiesPacket(
    val invulnerable: Boolean,
    val flying: Boolean,
    val allowFlying: Boolean,
    val creativeMode: Boolean,
    val flyingSpeed: Float = 0.05f,
    val fovModifier: Float = 1f
) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x31
    }

    override fun write(out: MinecraftOutputStream) {
        out.writeByte(
            (if (invulnerable) 0x01 else 0) or
            (if (flying) 0x02 else 0) or
            (if (allowFlying) 0x04 else 0) or
            (if (creativeMode) 0x08 else 0)
        )
        out.writeFloat(flyingSpeed)
        out.writeFloat(fovModifier)
    }
}
