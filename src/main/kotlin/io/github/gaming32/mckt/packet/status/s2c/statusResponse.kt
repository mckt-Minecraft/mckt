@file:UseSerializers(UUIDSerializer::class, TextSerializer::class)

package io.github.gaming32.mckt.packet.status.s2c

import io.github.gaming32.mckt.PlayClient
import io.github.gaming32.mckt.data.MinecraftOutputStream
import io.github.gaming32.mckt.objects.TextSerializer
import io.github.gaming32.mckt.objects.UUIDSerializer
import io.github.gaming32.mckt.packet.Packet
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import net.kyori.adventure.text.Component
import java.util.*

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class StatusResponse(
    val version: Version,
    val players: Players,
    val description: Component,
    val favicon: String? = null,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val previewsChat: Boolean = false,
    val enforcesSecureChat: Boolean
) {
    @Serializable
    data class Version(val name: String, val protocol: Int)

    @Serializable
    data class Players(
        val max: Int,
        val online: Int,
        @EncodeDefault(EncodeDefault.Mode.ALWAYS)
        val sample: List<Sample> = listOf()
    ) {
        @Serializable
        data class Sample(
            val name: String,
            val id: UUID
        ) {
            constructor(client: PlayClient) : this(client.username, client.uuid)
        }
    }
}

data class StatusResponsePacket(val status: StatusResponse) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x00
    }

    override fun write(out: MinecraftOutputStream) = out.writeString(Json.encodeToString(status))
}
