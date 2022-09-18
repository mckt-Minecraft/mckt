package io.github.gaming32.mckt.packet.play.s2c

import io.github.gaming32.mckt.data.MinecraftOutputStream
import io.github.gaming32.mckt.objects.Identifier
import io.github.gaming32.mckt.packet.Packet

class SyncTagsPacket(val tags: Map<Identifier, Map<Identifier, IntArray>>) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x6B
    }

    override fun write(out: MinecraftOutputStream) {
        out.writeVarInt(tags.size)
        tags.forEach { (type, actualTags) ->
            out.writeIdentifier(type)
            out.writeVarInt(actualTags.size)
            actualTags.forEach { (name, entries) ->
                out.writeIdentifier(name)
                out.writeVarInt(entries.size)
                entries.forEach { out.writeVarInt(it) }
            }
        }
    }
}
