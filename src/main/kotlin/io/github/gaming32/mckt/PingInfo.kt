package io.github.gaming32.mckt

import io.ktor.network.sockets.*

data class PingInfo(
    val protocolVersion: Int,
    val serverIp: String, val serverPort: Int,
    val clientAddress: SocketAddress
)
