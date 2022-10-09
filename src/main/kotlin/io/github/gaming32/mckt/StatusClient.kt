package io.github.gaming32.mckt

import io.github.gaming32.mckt.packet.PacketState
import io.github.gaming32.mckt.packet.status.StatusPingPacket
import io.github.gaming32.mckt.packet.status.c2s.StatusRequestPacket
import io.github.gaming32.mckt.packet.status.s2c.StatusResponse
import io.github.gaming32.mckt.packet.status.s2c.StatusResponsePacket
import io.ktor.network.sockets.*
import io.ktor.util.*
import io.ktor.utils.io.*
import java.io.File

class StatusClient(
    server: MinecraftServer,
    socket: Socket,
    receiveChannel: ByteReadChannel,
    sendChannel: ByteWriteChannel
) : Client(server, socket, receiveChannel, sendChannel) {
    companion object {
        private val FAVICON_FILE = File("icon.png")
    }

    override val primaryState = PacketState.STATUS

    suspend fun handle() = socket.use {
        readPacket<StatusRequestPacket>() ?: return
        val favicon = if (FAVICON_FILE.isFile) {
            "data:image/png;base64," + FAVICON_FILE.readBytes().encodeBase64()
        } else null
        sendPacket(StatusResponsePacket(
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
                favicon = favicon,
                previewsChat = false,
                enforcesSecureChat = false
            )
        ))
        sendPacket(readPacket<StatusPingPacket>() ?: return)
    }
}
