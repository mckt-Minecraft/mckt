viewDistance = 10
simulationDistance = 10
maxPlayers = 20
seed = null
defaultWorldGenerator = WorldGenerator.NORMAL
networkCompressionThreshold = 256
autosavePeriod = 5 * 60 * 20
enableVanillaClientSpoofAlerts = true
enableChatPreview = true

motd {
    text {
        append(text("My mckt server", TextColor.fromHexString("#36523d")))
        append(text("\nMy address is ", TextColor.fromCSSHexString("#999")))
        append(text(pingInfo.serverIp, NamedTextColor.GREEN))
    }
}

formatChat {
    text {
        val shiftAmount = 1f / message.length
        var hue = 0f
        for (c in message) {
            append(text(c, color(hsvLike(hue, 1f, 1f))))
            hue += shiftAmount
        }
    }
}
