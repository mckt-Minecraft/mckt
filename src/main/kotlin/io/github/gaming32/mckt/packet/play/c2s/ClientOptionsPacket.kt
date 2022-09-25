package io.github.gaming32.mckt.packet.play.c2s

import io.github.gaming32.mckt.PlayClient
import io.github.gaming32.mckt.data.*
import io.github.gaming32.mckt.packet.Packet
import java.io.InputStream
import java.io.OutputStream

data class ClientOptionsPacket(val options: PlayClient.ClientOptions) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x08
    }

    constructor(inp: InputStream) : this(PlayClient.ClientOptions(
        inp.readString(16),
        inp.readByte().toInt(),
        inp.readVarInt(),
        inp.readBoolean(),
        inp.readUByte().toInt(),
        inp.readEnum(),
        inp.readBoolean(),
        inp.readBoolean()
    ))

    override fun write(out: OutputStream) {
        out.writeString(options.locale, 16)
        out.writeByte(options.viewDistance)
        out.writeVarInt(options.chatMode)
        out.writeBoolean(options.chatColors)
        out.writeByte(options.displayedSkinParts)
        out.writeEnum(options.mainHand)
        out.writeBoolean(options.textFiltering)
        out.writeBoolean(options.allowServerListings)
    }
}
