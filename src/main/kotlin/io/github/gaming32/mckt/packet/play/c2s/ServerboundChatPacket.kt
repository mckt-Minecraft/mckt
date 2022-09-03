package io.github.gaming32.mckt.packet.play.c2s

import io.github.gaming32.mckt.packet.MinecraftInputStream
import io.github.gaming32.mckt.packet.MinecraftOutputStream
import io.github.gaming32.mckt.packet.Packet

class ServerboundChatPacket(
    val message: String,
    val timestamp: Long,
    val salt: Long,
    val signature: ByteArray,
    val signedPreview: Boolean
) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x05
    }

    constructor(inp: MinecraftInputStream) : this(
        inp.readString(256),
        inp.readLong(),
        inp.readLong(),
        ByteArray(inp.readVarInt()).also { inp.readFully(it) },
        inp.readBoolean()
    )

    override fun write(out: MinecraftOutputStream) {
        out.writeString(message, 256)
        out.writeLong(timestamp)
        out.writeLong(salt)
        out.writeVarInt(signature.size)
        out.write(signature)
        out.writeBoolean(signedPreview)
    }
}
