package io.github.gaming32.mckt.packet

import io.github.gaming32.mckt.packet.login.c2s.LoginStartPacket
import io.github.gaming32.mckt.packet.play.c2s.*
import io.github.gaming32.mckt.packet.status.PingPacket
import io.github.gaming32.mckt.packet.status.c2s.StatusRequestPacket
import io.ktor.utils.io.*
import kotlinx.coroutines.withTimeout
import java.io.ByteArrayInputStream

enum class PacketState(private val packets: Map<Int, (MinecraftInputStream) -> Packet>) {
    HANDSHAKE(mapOf()),
    STATUS(mapOf(
        /* 0x00 */ StatusRequestPacket.TYPE to ::StatusRequestPacket,
        /* 0x01 */ PingPacket.TYPE to ::PingPacket
    )),
    LOGIN(mapOf(
        /* 0x00 */ LoginStartPacket.TYPE to ::LoginStartPacket
    )),
    PLAY(mapOf(
        /* 0x00 */ ConfirmTeleportationPacket.TYPE to ::ConfirmTeleportationPacket,
        /* 0x04 */ CommandPacket.TYPE to ::CommandPacket,
        /* 0x05 */ ServerboundChatPacket.TYPE to ::ServerboundChatPacket,
        /* 0x08 */ ClientOptionsPacket.TYPE to ::ClientOptionsPacket,
        /* 0x0D */ ServerboundPlayPluginPacket.TYPE to ::ServerboundPlayPluginPacket,
        /* 0x14 */ PlayerPositionPacket.TYPE to ::PlayerPositionPacket,
        /* 0x15 */ PlayerPositionAndRotationPacket.TYPE to ::PlayerPositionAndRotationPacket,
        /* 0x16 */ PlayerRotationPacket.TYPE to ::PlayerRotationPacket,
        /* 0x17 */ PlayerOnGroundPacket.TYPE to ::PlayerOnGroundPacket,
        /* 0x1C */ ServerboundPlayerAbilitiesPacket.TYPE to ::ServerboundPlayerAbilitiesPacket
    ));

    suspend fun readPacket(channel: ByteReadChannel): Packet {
        val packetLength = channel.readVarInt()
        val bytesRead = channel.totalBytesRead
        val packetId = channel.readVarInt()
        val packetIdLength = (channel.totalBytesRead - bytesRead).toInt()
        val packetData = ByteArray(packetLength - packetIdLength)
        channel.readFully(packetData, 0, packetData.size)
        val reader = packets[packetId] ?: throw IllegalArgumentException(
            "Unknown packet ID for state $this: 0x${packetId.toString(16).padStart(2, '0')}"
        )
        return reader(MinecraftInputStream(ByteArrayInputStream(packetData)))
    }

    @JvmName("readSpecificPacketWithTimeout")
    suspend inline fun <reified T : Packet> readPacket(channel: ByteReadChannel): T? {
        val packet = withTimeout(5000) { readPacket(channel) }
        return if (packet is T) packet else null
    }
}
