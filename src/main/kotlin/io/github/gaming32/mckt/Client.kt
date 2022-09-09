package io.github.gaming32.mckt

import io.github.gaming32.mckt.packet.Packet
import io.github.gaming32.mckt.packet.PacketState
import io.github.gaming32.mckt.packet.sendPacket
import io.ktor.network.sockets.*
import io.ktor.utils.io.*

sealed class Client(
    val server: MinecraftServer,
    internal val socket: Socket,
    internal val receiveChannel: ByteReadChannel,
    @PublishedApi internal val sendChannel: ByteWriteChannel
) {
    protected abstract val primaryState: PacketState
    internal var compression: Int = -1

    internal suspend fun readPacket() = primaryState.readPacket(receiveChannel, compression >= 0)

    @JvmName("readSpecificPacketWithTimeout")
    internal suspend inline fun <reified T : Packet> readPacket() =
        primaryState.readPacket<T>(receiveChannel, compression >= 0)

    open suspend fun sendPacket(packet: Packet) = sendChannel.sendPacket(packet, compression)
}
