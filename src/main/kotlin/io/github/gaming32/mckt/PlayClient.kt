package io.github.gaming32.mckt

import io.github.gaming32.mckt.packet.PacketState
import io.github.gaming32.mckt.packet.login.c2s.LoginStartPacket
import io.github.gaming32.mckt.packet.login.s2c.LoginDisconnectPacket
import io.github.gaming32.mckt.packet.login.s2c.LoginSuccessPacket
import io.github.gaming32.mckt.packet.writePacket
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import net.kyori.adventure.text.Component
import java.util.*

class PlayClient(
    server: MinecraftServer,
    socket: Socket,
    receiveChannel: ByteReadChannel,
    sendChannel: ByteWriteChannel
) : Client(server, socket, receiveChannel, sendChannel) {
    companion object {
        private val LOGGER = getLogger()
    }

    override val primaryState = PacketState.PLAY

    lateinit var username: String
        private set
    lateinit var uuid: UUID
        private set
    lateinit var handlePacketsJob: Job

    suspend fun handshake() {
        val loginStart = PacketState.LOGIN.readPacket<LoginStartPacket>(receiveChannel)
        if (loginStart == null) {
            sendChannel.writePacket(LoginDisconnectPacket(Component.text("Unexpected packet")))
            socket.dispose()
            return
        }
        username = loginStart.username
        if (!(username matches USERNAME_REGEX)) {
            sendChannel.writePacket(
                LoginDisconnectPacket(Component.text("Username doesn't match regex $USERNAME_REGEX"))
            )
            socket.dispose()
            return
        }
        uuid = UUID.nameUUIDFromBytes("OfflinePlayer:$username".encodeToByteArray())
        sendChannel.writePacket(LoginSuccessPacket(uuid, username))
    }

    suspend fun handlePackets() {
        while (server.running) {
            val packet = try {
                readPacket()
            } catch (e: Exception) {
                if (e !is ClosedReceiveChannelException) {
                    LOGGER.warn("Client connection had error", e)
                }
                break
            }
            println(packet)
        }
    }
}
