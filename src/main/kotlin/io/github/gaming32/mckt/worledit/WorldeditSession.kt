package io.github.gaming32.mckt.worledit

import io.github.gaming32.mckt.PlayClient
import io.github.gaming32.mckt.objects.*
import net.kyori.adventure.text.Component
import java.util.*

class WorldeditSession(val client: PlayClient) {
    companion object {
        val CUI_CHANNEL = Identifier("worldedit", "cui")
        val MARKER_POS1 = Identifier("worldedit", "pos1")
        val MARKER_POS2 = Identifier("worldedit", "pos2")
        val MARKER_REGION = Identifier("worldedit", "region")
    }

    private var initializedCui = false
    private var useMarkers = false

    var pos1: BlockPosition? = null
        private set
    var pos2: BlockPosition? = null
        private set
    val selection: BlockBox? get() {
        return BlockBox(pos1 ?: return null, pos2 ?: return null)
    }

    var clipboard: WorldeditClipboard? = null

    suspend fun setPos1(pos: BlockPosition) {
        pos1 = pos
        client.sendMessage(Component.text("Set position 1 to ${pos.x}, ${pos.y}, ${pos.z}"))
        if (!initializedCui) resetCui()
        if (useMarkers) {
            client.addMarker(MARKER_POS1, BlockMarker(pos, 0xff0000ff.toInt(), "Position 1"))
            addRegionMarker()
        } else {
            syncCui(pos, 0)
        }
    }

    suspend fun setPos2(pos: BlockPosition) {
        pos2 = pos
        client.sendMessage(Component.text("Set position 2 to ${pos.x}, ${pos.y}, ${pos.z}"))
        if (!initializedCui) resetCui()
        if (useMarkers) {
            client.addMarker(MARKER_POS2, BlockMarker(pos, 0xffff0000.toInt(), "Position 2"))
            addRegionMarker()
        } else {
            syncCui(pos, 1)
        }
    }

    suspend fun clear() {
        pos1 = null
        pos2 = null
        if (!initializedCui) resetCui()
        if (useMarkers) {
            client.removeMarkers(MARKER_POS1, MARKER_POS2, MARKER_REGION)
        } else {
            resetCui()
        }
    }

    private suspend fun resetCui() {
        initializedCui = true
        if (CUI_CHANNEL !in client.supportedChannels) {
            useMarkers = true
            return
        }
        client.sendCustomPacket(CUI_CHANNEL) {
            write("s|cuboid".toByteArray())
        }
    }

    private suspend fun syncCui(pos: BlockPosition?, index: Int) {
        if (pos == null) return
        client.sendCustomPacket(CUI_CHANNEL) {
            write("p|$index|${pos.x}|${pos.y}|${pos.z}|${selection?.volume ?: -1}".toByteArray())
        }
    }

    private suspend fun addRegionMarker() {
        val region = selection ?: return
        if (region.volume < 1000) {
            client.addMarker(MARKER_REGION, BlockBoxMarker(region, 0x6000ff00))
        } else {
            client.removeMarkers(MARKER_REGION)
        }
    }
}

private val clientToSession = WeakHashMap<PlayClient, WorldeditSession>()

val PlayClient.worldeditSession get() = clientToSession.computeIfAbsent(this) { WorldeditSession(it) }!!
