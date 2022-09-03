package io.github.gaming32.mckt.packet.login.c2s

import io.github.gaming32.mckt.packet.MinecraftInputStream
import io.github.gaming32.mckt.packet.MinecraftOutputStream
import io.github.gaming32.mckt.packet.Packet
import java.util.UUID

class LoginStartPacket(
    val username: String,
    val signatureInfo: SignatureInfo? = null,
    val uuid: UUID? = null
) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x00
    }

    class SignatureInfo(val timestamp: Long, val publicKey: ByteArray, val signature: ByteArray)

    constructor(inp: MinecraftInputStream) : this(
        inp.readString(16),
        if (!inp.readBoolean()) null else inp.run { SignatureInfo(
            readLong(),
            ByteArray(readVarInt()).also { readFully(it) },
            ByteArray(readVarInt()).also { readFully(it) }
        ) },
        if (!inp.readBoolean()) null else inp.readUuid()
    )

    override fun write(out: MinecraftOutputStream) {
        out.writeString(username, 16)
        out.writeBoolean(signatureInfo != null)
        if (signatureInfo != null) {
            out.writeLong(signatureInfo.timestamp)
            out.writeVarInt(signatureInfo.publicKey.size)
            out.write(signatureInfo.publicKey)
            out.writeVarInt(signatureInfo.signature.size)
            out.write(signatureInfo.signature)
        }
        out.writeBoolean(uuid != null)
        if (uuid != null) {
            out.writeUuid(uuid)
        }
    }
}
