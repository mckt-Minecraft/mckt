package io.github.gaming32.mckt

import io.github.gaming32.mckt.packet.PacketState
import io.github.gaming32.mckt.packet.sendPacket
import io.github.gaming32.mckt.packet.status.StatusPingPacket
import io.github.gaming32.mckt.packet.status.c2s.StatusRequestPacket
import io.github.gaming32.mckt.packet.status.s2c.StatusResponse
import io.github.gaming32.mckt.packet.status.s2c.StatusResponsePacket
import io.ktor.network.sockets.*
import io.ktor.utils.io.*

class StatusClient(
    server: MinecraftServer,
    socket: Socket,
    receiveChannel: ByteReadChannel,
    sendChannel: ByteWriteChannel
) : Client(server, socket, receiveChannel, sendChannel) {
    override val primaryState = PacketState.STATUS

    suspend fun handle() = socket.use {
        readPacket<StatusRequestPacket>() ?: return
        sendChannel.sendPacket(StatusResponsePacket(
            StatusResponse(
                version = StatusResponse.Version(
                    name = "1.19.2",
                    protocol = 760
                ),
                players = StatusResponse.Players(
                    max = server.config.maxPlayers,
                    online = server.clients.size,
                    sample = server.clients.values.asSequence()
                        .filter { it.options.allowServerListings }
                        .take(12)
                        .map(StatusResponse.Players::Sample)
                        .toList()
                ),
                description = server.config.motd,
                previewsChat = false,
                enforcesSecureChat = false
            )
        ))
        sendChannel.sendPacket(readPacket<StatusPingPacket>() ?: return)
    }
}
