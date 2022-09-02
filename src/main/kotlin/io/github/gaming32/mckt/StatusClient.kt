package io.github.gaming32.mckt

import io.github.gaming32.mckt.packet.PacketState
import io.github.gaming32.mckt.packet.status.PingPacket
import io.github.gaming32.mckt.packet.status.c2s.StatusRequestPacket
import io.github.gaming32.mckt.packet.status.s2c.StatusResponse
import io.github.gaming32.mckt.packet.status.s2c.StatusResponsePacket
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.withTimeout
import net.kyori.adventure.text.Component

class StatusClient(
    private val socket: Socket,
    private val receiveChannel: ByteReadChannel,
    private val sendChannel: ByteWriteChannel
) {
    suspend fun handle() {
        val requestPacket = withTimeout(5000L) { readPacket() }
        if (requestPacket !is StatusRequestPacket) {
            socket.dispose()
            return
        }
        StatusResponsePacket(
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
        ).writePacket(sendChannel)
        val pingPacket = try {
            withTimeout(5000L) { readPacket() }
        } catch (e: CancellationException) {
            socket.dispose()
            return
        }
        if (pingPacket !is PingPacket) {
            socket.dispose()
            return
        }
        pingPacket.writePacket(sendChannel)
    }

    private suspend fun readPacket() = PacketState.STATUS.readPacket(receiveChannel)
}
