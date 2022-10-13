package io.github.gaming32.mckt.packet.play

import io.github.gaming32.mckt.data.encodeData
import io.github.gaming32.mckt.data.readIdentifier
import io.github.gaming32.mckt.data.writeIdentifier
import io.github.gaming32.mckt.objects.Identifier
import io.github.gaming32.mckt.packet.Packet
import java.io.InputStream
import java.io.OutputStream
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class PlayCustomPacket(val channel: Identifier, val data: ByteArray) : Packet(S2C_TYPE) {
    companion object {
        const val S2C_TYPE = 0x16
        const val C2S_TYPE = 0x0D
    }

    constructor(inp: InputStream) : this(
        inp.readIdentifier(),
        inp.readBytes()
    )

    override fun write(out: OutputStream) {
        out.writeIdentifier(channel)
        out.write(data)
    }

    override fun toString() = "PlayPluginPacket(channel=$channel, data=...)"
}

@OptIn(ExperimentalContracts::class)
inline fun PlayCustomPacket(channel: Identifier, builder: OutputStream.() -> Unit): PlayCustomPacket {
    contract {
        callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
    }
    return PlayCustomPacket(channel, encodeData(builder))
}
