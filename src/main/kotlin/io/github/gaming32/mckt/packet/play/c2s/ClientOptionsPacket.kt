package io.github.gaming32.mckt.packet.play.c2s

import io.github.gaming32.mckt.PlayClient
import io.github.gaming32.mckt.packet.MinecraftInputStream
import io.github.gaming32.mckt.packet.MinecraftOutputStream
import io.github.gaming32.mckt.packet.Packet

class ClientOptionsPacket(val options: PlayClient.ClientOptions) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x08
    }

    constructor(inp: MinecraftInputStream) : this(PlayClient.ClientOptions(
        inp.readString(16),
        inp.readByte().toInt(),
        inp.readVarInt(),
        inp.readBoolean(),
        inp.readUnsignedByte(),
        inp.readVarInt(),
        inp.readBoolean(),
        inp.readBoolean()
    ))

    override fun write(out: MinecraftOutputStream) {
        out.writeString(options.locale, 16)
        out.writeByte(options.viewDistance)
        out.writeVarInt(options.chatMode)
        out.writeBoolean(options.chatColors)
        out.writeByte(options.displayedSkinParts)
        out.writeVarInt(options.mainHand)
        out.writeBoolean(options.textFiltering)
        out.writeBoolean(options.allowServerListings)
    }
}
