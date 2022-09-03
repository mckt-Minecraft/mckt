package io.github.gaming32.mckt

import io.github.gaming32.mckt.packet.PacketState
import io.github.gaming32.mckt.packet.status.PingPacket
import io.github.gaming32.mckt.packet.status.c2s.StatusRequestPacket
import io.github.gaming32.mckt.packet.status.s2c.StatusResponse
import io.github.gaming32.mckt.packet.status.s2c.StatusResponsePacket
import io.github.gaming32.mckt.packet.sendPacket
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import net.kyori.adventure.text.Component

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
                    max = 20,
                    online = server.clients.size,
                    sample = server.clients.values.asSequence()
                        .filter { it.options.allowServerListings }
                        .take(12)
                        .map(StatusResponse.Players::Sample)
                        .toList()
                ),
                description = Component.text("mckt test server"),
                previewsChat = false,
                enforcesSecureChat = false
            )
        ))
        sendChannel.sendPacket(readPacket<PingPacket>() ?: return)
    }
}
