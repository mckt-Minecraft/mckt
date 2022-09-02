package io.github.gaming32.mckt.packet.status.s2c

import io.github.gaming32.mckt.objects.TextSerializer
import io.github.gaming32.mckt.objects.UUIDSerializer
import io.github.gaming32.mckt.packet.MinecraftOutputStream
import io.github.gaming32.mckt.packet.Packet
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.kyori.adventure.text.Component
import java.util.*

@Serializable
@OptIn(ExperimentalSerializationApi::class)
class StatusResponse(
    val version: Version,
    val players: Players,
    @Serializable(with = TextSerializer::class)
    val description: Component,
    val favicon: String? = null,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val previewsChat: Boolean = false,
    val enforcesSecureChat: Boolean
) {
    @Serializable
    class Version(val name: String, val protocol: Int)

    @Serializable
    class Players(
        val max: Int,
        val online: Int,
        @EncodeDefault(EncodeDefault.Mode.ALWAYS)
        val sample: List<Sample> = listOf()
    ) {
        @Serializable
        class Sample(
            val name: String,
            @Serializable(with = UUIDSerializer::class)
            val id: UUID
        )
    }
}

class StatusResponsePacket(val status: StatusResponse) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x00
    }

    override fun write(out: MinecraftOutputStream) = out.writeString(Json.encodeToString(status))
}
