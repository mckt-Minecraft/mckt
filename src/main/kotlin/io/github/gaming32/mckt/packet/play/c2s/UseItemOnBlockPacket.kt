package io.github.gaming32.mckt.packet.play.c2s

import io.github.gaming32.mckt.data.*
import io.github.gaming32.mckt.objects.BlockHitResult
import io.github.gaming32.mckt.objects.Hand
import io.github.gaming32.mckt.packet.Packet
import java.io.InputStream
import java.io.OutputStream

data class UseItemOnBlockPacket(
    val hand: Hand,
    val hit: BlockHitResult,
    val sequence: Int
) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x31
    }

    constructor(inp: InputStream) : this(
        inp.readEnum(),
        inp.readBlockHitResult(),
        inp.readVarInt()
    )

    override fun write(out: OutputStream) {
        out.writeEnum(hand)
        out.writeBlockHitResult(hit)
        out.writeVarInt(sequence)
    }
}
