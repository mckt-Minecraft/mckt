package io.github.gaming32.mckt.packet.play.s2c

import io.github.gaming32.mckt.Gamemode
import io.github.gaming32.mckt.packet.MinecraftOutputStream
import io.github.gaming32.mckt.packet.Packet
import net.kyori.adventure.text.Component
import java.util.*

class PlayerListUpdatePacket(vararg val actions: Action) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x37
    }

    init {
        if (actions.isNotEmpty()) {
            val uniformType = actions[0].type
            for (i in 1 until actions.size) {
                if (actions[i].type != uniformType) {
                    throw IllegalArgumentException("PlayerListUpdatePacket action type not uniform")
                }
            }
        }
    }

    sealed class Action(val type: Int, val uuid: UUID) {
        abstract fun write(out: MinecraftOutputStream)
    }

    class AddPlayer(
        uuid: UUID,
        val name: String,
        val properties: Map<String, Pair<String, String?>>,
        val gamemode: Gamemode,
        val ping: Int,
        val displayName: Component?,
        val signatureData: SignatureData?
    ) : Action(0, uuid) {
        class SignatureData(
            val timestamp: Long,
            val publicKey: ByteArray?,
            val signature: ByteArray?
        ) {
            override fun toString() = "SignatureData(timestamp=$timestamp, publicKey=..., signature=...)"
        }

        override fun write(out: MinecraftOutputStream) {
            out.writeString(name, 16)
            out.writeVarInt(properties.size)
            properties.forEach { name, (value, signature) ->
                out.writeString(name)
                out.writeString(value)
                out.writeBoolean(signature != null)
                signature?.let { out.writeString(it) }
            }
            out.writeVarInt(gamemode.ordinal)
            out.writeVarInt(ping)
            out.writeBoolean(displayName != null)
            displayName?.let { out.writeText(it) }
            out.writeBoolean(signatureData != null)
            signatureData?.let {
                out.writeLong(it.timestamp)
                out.writeVarInt(it.publicKey?.size ?: 0)
                it.publicKey?.let { key -> out.write(key) }
                out.writeVarInt(it.signature?.size ?: 0)
                it.signature?.let { sig -> out.write(sig) }
            }
        }

        override fun toString() =
            "AddPlayer(uuid=$uuid, name=$name, properties=$properties, gamemode=$gamemode, ping=${ping}, " +
                "displayName=$displayName, signatureData=$signatureData)"
    }

    class UpdateGamemode(uuid: UUID, val gamemode: Gamemode) : Action(1, uuid) {
        override fun write(out: MinecraftOutputStream) = out.writeVarInt(gamemode.ordinal)

        override fun toString() = "UpdateGamemode(uuid=$uuid, gamemode=$gamemode)"
    }

    class UpdatePing(uuid: UUID, val ping: Int) : Action(2, uuid) {
        override fun write(out: MinecraftOutputStream) = out.writeVarInt(ping)

        override fun toString() = "UpdatePing(uuid=$uuid, ping=$ping)"
    }

    class UpdateDisplayName(uuid: UUID, val displayName: Component?) : Action(3, uuid) {
        override fun write(out: MinecraftOutputStream) {
            out.writeBoolean(displayName != null)
            displayName?.let { out.writeText(it) }
        }

        override fun toString() = "UpdateDisplayName(uuid=$uuid, displayName=$displayName)"
    }

    class RemovePlayer(uuid: UUID) : Action(4, uuid) {
        override fun write(out: MinecraftOutputStream) = Unit

        override fun toString() = "RemovePlayer(uuid=$uuid)"
    }

    override fun write(out: MinecraftOutputStream) {
        if (actions.isEmpty()) {
            out.writeVarInt(0)
            out.writeVarInt(0)
            return
        }
        out.writeVarInt(actions[0].type)
        out.writeVarInt(actions.size)
        actions.forEach { action ->
            out.writeUuid(action.uuid)
            action.write(out)
        }
    }

    override fun toString() = "PlayerListUpdatePacket(actions=${actions.contentToString()})"
}
