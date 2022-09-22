package io.github.gaming32.mckt.packet.play.c2s

import io.github.gaming32.mckt.data.readEnum
import io.github.gaming32.mckt.data.readVarInt
import io.github.gaming32.mckt.data.writeEnum
import io.github.gaming32.mckt.data.writeVarInt
import io.github.gaming32.mckt.objects.Hand
import io.github.gaming32.mckt.packet.Packet
import java.io.InputStream
import java.io.OutputStream

data class UseItemPacket(val hand: Hand, val sequence: Int) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x32
    }

    constructor(inp: InputStream) : this(
        inp.readEnum(),
        inp.readVarInt()
    )

    override fun write(out: OutputStream) {
        out.writeEnum(hand)
        out.writeVarInt(sequence)
    }
}
