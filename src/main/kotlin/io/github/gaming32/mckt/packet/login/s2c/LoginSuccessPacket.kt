package io.github.gaming32.mckt.packet.login.s2c

import io.github.gaming32.mckt.data.MinecraftOutputStream
import io.github.gaming32.mckt.packet.Packet
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

    override fun write(out: MinecraftOutputStream) {
        out.writeUuid(uuid)
        out.writeString(username, 16)
        out.writeVarInt(properties.size)
        properties.forEach { property ->
            out.writeString(property.name)
            out.writeString(property.value)
            out.writeBoolean(property.signature != null)
            property.signature?.let { out.writeString(it) }
        }
    }
}
