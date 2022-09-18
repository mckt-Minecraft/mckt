package io.github.gaming32.mckt.packet.play.s2c

import io.github.gaming32.mckt.data.writeArray
import io.github.gaming32.mckt.data.writeIdentifier
import io.github.gaming32.mckt.data.writeVarIntArray
import io.github.gaming32.mckt.objects.Identifier
import io.github.gaming32.mckt.packet.Packet
import java.io.OutputStream

class SyncTagsPacket(val tags: Map<Identifier, Map<Identifier, IntArray>>) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x6B
    }

    override fun write(out: OutputStream) {
        out.writeArray(tags) { type, actualTags ->
            writeIdentifier(type)
            writeArray(actualTags) { name, entries ->
                writeIdentifier(name)
                writeVarIntArray(entries)
            }
        }
    }
}
