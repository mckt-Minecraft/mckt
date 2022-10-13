viewDistance = 10
simulationDistance = 10
maxPlayers = 20
seed = null
defaultWorldGenerator = id("mckt:default")
networkCompressionThreshold = 256
autosavePeriod = 5 * 60 * 20
enableVanillaClientSpoofAlerts = true
enableChatPreview = true

motd {
    text {
        append(text("My mckt server", color("#36523d")))
        append(text("\nMy address is ", color("#999")))
        append(text(pingInfo.serverIp, color("green")))
    }
}

formatChat {
    text {
        val shiftAmount = 1f / message.length
        var hue = 0f
        for (c in message) {
            append(text(c, hsv(hue, 1f, 1f)))
            hue += shiftAmount
        }
    }
}
