package io.github.gaming32.mckt.packet.play.s2c

import io.github.gaming32.mckt.data.writeLong
import io.github.gaming32.mckt.packet.Packet
import java.io.OutputStream
import kotlin.math.absoluteValue

data class UpdateTimePacket(val worldTime: Long, val freezeDayTime: Long? = null) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x5C
    }

    override fun write(out: OutputStream) {
        out.writeLong(worldTime)
        if (freezeDayTime == null) {
            out.writeLong(worldTime)
        } else {
            out.writeLong(-freezeDayTime.absoluteValue)
        }
    }
}
