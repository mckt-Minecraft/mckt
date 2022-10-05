package io.github.gaming32.mckt.packet.login.s2c

import io.github.gaming32.mckt.data.encodeData
import io.github.gaming32.mckt.data.writeIdentifier
import io.github.gaming32.mckt.data.writeVarInt
import io.github.gaming32.mckt.objects.Identifier
import io.github.gaming32.mckt.packet.Packet
import java.io.OutputStream

class LoginPluginRequestPacket(val messageId: Int, val channel: Identifier, val data: ByteArray) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x04
    }

    override fun write(out: OutputStream) {
        out.writeVarInt(messageId)
        out.writeIdentifier(channel)
        out.write(data)
    }
}

inline fun LoginPluginRequestPacket(messageId: Int, channel: Identifier, builder: OutputStream.() -> Unit) =
    LoginPluginRequestPacket(messageId, channel, encodeData(builder))
