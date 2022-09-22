package io.github.gaming32.mckt.packet.play.s2c

import io.github.gaming32.mckt.Gamemode
import io.github.gaming32.mckt.data.*
import io.github.gaming32.mckt.packet.Packet
import net.kyori.adventure.text.Component
import java.io.OutputStream
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

    sealed class Action(val type: Int, val uuid: UUID) : Writable

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

        override fun write(out: OutputStream) {
            out.writeString(name, 16)
            out.writeArray(properties) { name, (value, signature) ->
                writeString(name)
                writeString(value)
                writeOptionalString(signature)
            }
            out.writeEnum(gamemode)
            out.writeVarInt(ping)
            out.writeOptionalText(displayName)
            out.writeOptional(signatureData) {
                writeLong(it.timestamp)
                writeVarInt(it.publicKey?.size ?: 0)
                it.publicKey?.let { key -> write(key) }
                writeVarInt(it.signature?.size ?: 0)
                it.signature?.let { sig -> write(sig) }
            }
        }

        override fun toString() =
            "AddPlayer(uuid=$uuid, name=$name, properties=$properties, gamemode=$gamemode, ping=${ping}, " +
                "displayName=$displayName, signatureData=$signatureData)"
    }

    class UpdateGamemode(uuid: UUID, val gamemode: Gamemode) : Action(1, uuid) {
        override fun write(out: OutputStream) = out.writeEnum(gamemode)

        override fun toString() = "UpdateGamemode(uuid=$uuid, gamemode=$gamemode)"
    }

    class UpdatePing(uuid: UUID, val ping: Int) : Action(2, uuid) {
        override fun write(out: OutputStream) = out.writeVarInt(ping)

        override fun toString() = "UpdatePing(uuid=$uuid, ping=$ping)"
    }

    class UpdateDisplayName(uuid: UUID, val displayName: Component?) : Action(3, uuid) {
        override fun write(out: OutputStream) = out.writeOptionalText(displayName)

        override fun toString() = "UpdateDisplayName(uuid=$uuid, displayName=$displayName)"
    }

    class RemovePlayer(uuid: UUID) : Action(4, uuid) {
        override fun write(out: OutputStream) = Unit

        override fun toString() = "RemovePlayer(uuid=$uuid)"
    }

    override fun write(out: OutputStream) {
        if (actions.isEmpty()) {
            out.writeVarInt(0)
            out.writeVarInt(0)
            return
        }
        out.writeVarInt(actions[0].type)
        out.writeArray(actions) { action ->
            writeUuid(action.uuid)
            action.write(this)
        }
    }

    override fun toString() = "PlayerListUpdatePacket(actions=${actions.contentToString()})"
}
