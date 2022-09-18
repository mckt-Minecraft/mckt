package io.github.gaming32.mckt.packet

import io.github.gaming32.mckt.data.readVarInt
import io.github.gaming32.mckt.getLogger
import io.github.gaming32.mckt.packet.login.c2s.LoginStartPacket
import io.github.gaming32.mckt.packet.play.PlayPingPacket
import io.github.gaming32.mckt.packet.play.PlayPluginPacket
import io.github.gaming32.mckt.packet.play.c2s.*
import io.github.gaming32.mckt.packet.status.StatusPingPacket
import io.github.gaming32.mckt.packet.status.c2s.StatusRequestPacket
import io.ktor.utils.io.*
import kotlinx.coroutines.withTimeout
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.Inflater

private val INFLATER = Inflater()

enum class PacketState(private val packets: Map<Int, (InputStream) -> Packet>) {
    HANDSHAKE(mapOf()),
    STATUS(mapOf(
        /* 0x00 */ StatusRequestPacket.TYPE to ::StatusRequestPacket,
        /* 0x01 */ StatusPingPacket.TYPE to ::StatusPingPacket
    )),
    LOGIN(mapOf(
        /* 0x00 */ LoginStartPacket.TYPE to ::LoginStartPacket
    )),
    PLAY(mapOf(
        /* 0x00 */ ConfirmTeleportationPacket.TYPE to ::ConfirmTeleportationPacket,
        /* 0x04 */ CommandPacket.TYPE to ::CommandPacket,
        /* 0x05 */ ServerboundChatPacket.TYPE to ::ServerboundChatPacket,
        /* 0x08 */ ClientOptionsPacket.TYPE to ::ClientOptionsPacket,
        /* 0x09 */ CommandCompletionsRequestPacket.TYPE to ::CommandCompletionsRequestPacket,
        /* 0x0D */ PlayPluginPacket.C2S_TYPE to ::PlayPluginPacket,
        /* 0x14 */ PlayerPositionPacket.TYPE to ::PlayerPositionPacket,
        /* 0x15 */ PlayerPositionAndRotationPacket.TYPE to ::PlayerPositionAndRotationPacket,
        /* 0x16 */ PlayerRotationPacket.TYPE to ::PlayerRotationPacket,
        /* 0x17 */ PlayerOnGroundPacket.TYPE to ::PlayerOnGroundPacket,
        /* 0x1C */ ServerboundPlayerAbilitiesPacket.TYPE to ::ServerboundPlayerAbilitiesPacket,
        /* 0x1D */ PlayerActionPacket.TYPE to ::PlayerActionPacket,
        /* 0x1E */ PlayerCommandPacket.TYPE to ::PlayerCommandPacket,
        /* 0x20 */ PlayPingPacket.C2S_TYPE to ::PlayPingPacket,
        /* 0x20 */ ServerboundSetHeldItemPacket.TYPE to ::ServerboundSetHeldItemPacket,
        /* 0x2B */ SetCreativeInventorySlotPacket.TYPE to ::SetCreativeInventorySlotPacket,
        /* 0x2F */ SwingArmPacket.TYPE to ::SwingArmPacket,
        /* 0x31 */ UseItemOnBlockPacket.TYPE to ::UseItemOnBlockPacket
    ));

    companion object {
        private val LOGGER = getLogger()
    }

    suspend fun readPacket(channel: ByteReadChannel, compression: Boolean): Packet {
        val totalPacketLength = channel.readVarInt()
        val packetInput: InputStream
        if (compression) {
            val bytesRead = channel.totalBytesRead
            val uncompressedLength = channel.readVarInt()
            if (uncompressedLength == 0) {
                val packetData = ByteArray(totalPacketLength - 1)
                channel.readFully(packetData)
                packetInput = ByteArrayInputStream(packetData)
            } else {
                val compressedData = ByteArray(totalPacketLength - (channel.totalBytesRead - bytesRead).toInt())
                channel.readFully(compressedData)
                INFLATER.setInput(compressedData)
                val result = ByteArray(uncompressedLength)
                var index = 0
                while (!INFLATER.finished()) {
                    index += INFLATER.inflate(result, index, result.size - index)
                }
                INFLATER.reset()
                packetInput = ByteArrayInputStream(result)
            }
        } else {
            val packetData = ByteArray(totalPacketLength)
            channel.readFully(packetData)
            packetInput = ByteArrayInputStream(packetData)
        }
        val packetId = packetInput.readVarInt()
        val reader = packets[packetId] ?: throw IllegalArgumentException(
            "Unknown packet ID for state $this: 0x${packetId.toString(16).padStart(2, '0')}"
        )
        return reader(packetInput).also { LOGGER.debug("Received packet {}", it) }
    }

    @JvmName("readSpecificPacketWithTimeout")
    suspend inline fun <reified T : Packet> readPacket(channel: ByteReadChannel, compression: Boolean): T? {
        val packet = withTimeout(5000) { readPacket(channel, compression) }
        return if (packet is T) packet else null
    }
}
