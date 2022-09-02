package io.github.gaming32.mckt.packet

import io.github.gaming32.mckt.packet.status.PingPacket
import io.github.gaming32.mckt.packet.status.c2s.StatusRequestPacket
import io.ktor.utils.io.*
import java.io.ByteArrayInputStream

enum class PacketState(private val packets: Map<Int, (MinecraftInputStream) -> Packet>) {
    HANDSHAKE(mapOf()),
    STATUS(mapOf(
        StatusRequestPacket.TYPE to ::StatusRequestPacket,
        PingPacket.TYPE to ::PingPacket
    )),
    LOGIN(mapOf()),
    PLAY(mapOf());

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
}
