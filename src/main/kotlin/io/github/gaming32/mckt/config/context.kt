package io.github.gaming32.mckt.config

import io.github.gaming32.mckt.MinecraftServer
import io.github.gaming32.mckt.PingInfo
import io.github.gaming32.mckt.PlayClient

data class MotdCreationContext(val server: MinecraftServer, val pingInfo: PingInfo)

data class ChatFormatContext(val sender: PlayClient, val message: String)
