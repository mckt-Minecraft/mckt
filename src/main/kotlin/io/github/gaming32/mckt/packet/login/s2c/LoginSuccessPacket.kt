package io.github.gaming32.mckt.packet.login.s2c

import io.github.gaming32.mckt.data.writeArray
import io.github.gaming32.mckt.data.writeBoolean
import io.github.gaming32.mckt.data.writeString
import io.github.gaming32.mckt.data.writeUuid
import io.github.gaming32.mckt.packet.Packet
import java.io.OutputStream
import java.util.*

data class LoginSuccessPacket(
    val uuid: UUID,
    val username: String,
    val properties: List<Property> = listOf()
) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x02
    }

    class Property(val name: String, val value: String, val signature: String? = null)

    override fun write(out: OutputStream) {
        out.writeUuid(uuid)
        out.writeString(username, 16)
        out.writeArray(properties) { property ->
            writeString(property.name)
            writeString(property.value)
            writeBoolean(property.signature != null)
            property.signature?.let { writeString(it) }
        }
    }
}
