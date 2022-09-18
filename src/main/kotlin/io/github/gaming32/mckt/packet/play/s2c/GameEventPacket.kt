package io.github.gaming32.mckt.packet.play.s2c

import io.github.gaming32.mckt.data.writeByte
import io.github.gaming32.mckt.data.writeFloat
import io.github.gaming32.mckt.packet.Packet
import java.io.OutputStream

data class GameEventPacket(val event: UByte, val value: Float) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x1D
        const val NO_RESPAWN_POINT: UByte = 0u
        const val END_RAIN: UByte = 1u
        const val BEGIN_RAIN: UByte = 2u
        const val SET_GAMEMODE: UByte = 3u
        const val WIN_GAME: UByte = 4u
        const val DEMO_EVENT: UByte = 5u
        const val ARROW_HIT_PLAYER: UByte = 6u
        const val RAIN_LEVEL: UByte = 7u
        const val THUNDER_LEVEL: UByte = 8u
        const val PLAY_PUFFERFISH_STING: UByte = 9u
        const val PLAY_ELDER_GUARDIAN: UByte = 10u
        const val TOGGLE_RESPAWN_SCREEN: UByte = 11u
    }

    override fun write(out: OutputStream) {
        out.writeByte(event.toInt())
        out.writeFloat(value)
    }
}
