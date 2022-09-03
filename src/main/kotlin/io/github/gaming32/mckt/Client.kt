package io.github.gaming32.mckt

import io.github.gaming32.mckt.packet.Packet
import io.github.gaming32.mckt.packet.PacketState
import io.ktor.network.sockets.*
import io.ktor.utils.io.*

sealed class Client(
    protected val server: MinecraftServer,
    internal val socket: Socket,
    internal val receiveChannel: ByteReadChannel,
    internal val sendChannel: ByteWriteChannel
) {
    protected abstract val primaryState: PacketState

    internal suspend fun readPacket() = primaryState.readPacket(receiveChannel)

    @JvmName("readSpecificPacketWithTimeout")
    internal suspend inline fun <reified T : Packet> readPacket() = primaryState.readPacket<T>(receiveChannel)
}
