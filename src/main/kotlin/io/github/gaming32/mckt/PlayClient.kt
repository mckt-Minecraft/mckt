package io.github.gaming32.mckt

import io.github.gaming32.mckt.packet.Packet
import io.github.gaming32.mckt.packet.PacketState
import io.github.gaming32.mckt.packet.login.c2s.LoginStartPacket
import io.github.gaming32.mckt.packet.login.s2c.LoginDisconnectPacket
import io.github.gaming32.mckt.packet.login.s2c.LoginSuccessPacket
import io.github.gaming32.mckt.packet.writePacket
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.delay
import net.kyori.adventure.text.Component
import java.util.UUID

class PlayClient(
    private val socket: Socket,
    private val receiveChannel: ByteReadChannel,
    private val sendChannel: ByteWriteChannel
) {
    suspend fun handshake() {
        val loginStart = PacketState.LOGIN.readPacket<LoginStartPacket>(receiveChannel)
        if (loginStart == null) {
            sendChannel.writePacket(LoginDisconnectPacket(Component.text("Unexpected packet")))
            socket.dispose()
            return
        }
        val username = loginStart.username
        if (!(username matches USERNAME_REGEX)) {
            sendChannel.writePacket(
                LoginDisconnectPacket(Component.text("Username doesn't match regex $USERNAME_REGEX"))
            )
            socket.dispose()
            return
        }
        val uuid = UUID.nameUUIDFromBytes("OfflinePlayer:$username".encodeToByteArray())
        sendChannel.writePacket(LoginSuccessPacket(uuid, username))
        delay(5000)
        socket.dispose()
    }

    private suspend inline fun <reified T : Packet> readPacket() = PacketState.PLAY.readPacket<T>(receiveChannel)
}
