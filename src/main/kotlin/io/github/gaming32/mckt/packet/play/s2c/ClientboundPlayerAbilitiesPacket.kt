package io.github.gaming32.mckt.packet.play.s2c

import io.github.gaming32.mckt.PlayerAbilities
import io.github.gaming32.mckt.data.writeByte
import io.github.gaming32.mckt.data.writeFloat
import io.github.gaming32.mckt.packet.Packet
import java.io.OutputStream

data class ClientboundPlayerAbilitiesPacket(val abilities: PlayerAbilities) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x31
    }

    override fun write(out: OutputStream) {
        out.writeByte(
            (if (abilities.invulnerable) 0x01 else 0) or
            (if (abilities.flying) 0x02 else 0) or
            (if (abilities.allowFlying) 0x04 else 0) or
            (if (abilities.creativeMode) 0x08 else 0)
        )
        out.writeFloat(abilities.flySpeed)
        out.writeFloat(abilities.walkSpeed)
    }
}
