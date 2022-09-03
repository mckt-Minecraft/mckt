package io.github.gaming32.mckt

import io.github.gaming32.mckt.packet.Packet
import io.github.gaming32.mckt.packet.PacketState
import io.github.gaming32.mckt.packet.status.PingPacket
import io.github.gaming32.mckt.packet.status.c2s.StatusRequestPacket
import io.github.gaming32.mckt.packet.status.s2c.StatusResponse
import io.github.gaming32.mckt.packet.status.s2c.StatusResponsePacket
import io.github.gaming32.mckt.packet.writePacket
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import net.kyori.adventure.text.Component

class StatusClient(
    private val socket: Socket,
    private val receiveChannel: ByteReadChannel,
    private val sendChannel: ByteWriteChannel
) {
    suspend fun handle() = socket.use {
        readPacket<StatusRequestPacket>() ?: return
        sendChannel.writePacket(StatusResponsePacket(
            StatusResponse(
                version = StatusResponse.Version(
                    name = "1.19.2",
                    protocol = 760
                ),
                players = StatusResponse.Players(
                    max = 1,
                    online = 0,
                    sample = listOf()
                ),
                description = Component.text("mckt test server"),
                previewsChat = false,
                enforcesSecureChat = false
            )
        ))
        sendChannel.writePacket(readPacket<PingPacket>() ?: return)
    }

    private suspend inline fun <reified T : Packet> readPacket() = PacketState.STATUS.readPacket<T>(receiveChannel)
}
