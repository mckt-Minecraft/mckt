package io.github.gaming32.mckt.packet.play.s2c

import com.mojang.brigadier.suggestion.Suggestions
import io.github.gaming32.mckt.commands.unwrap
import io.github.gaming32.mckt.data.writeBoolean
import io.github.gaming32.mckt.data.writeString
import io.github.gaming32.mckt.data.writeText
import io.github.gaming32.mckt.data.writeVarInt
import io.github.gaming32.mckt.packet.Packet
import java.io.OutputStream

data class CommandCompletionsResponsePacket(val requestId: Int, val suggestions: Suggestions) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x0E
    }

    override fun write(out: OutputStream) {
        out.writeVarInt(requestId)
        out.writeVarInt(suggestions.range.start)
        out.writeVarInt(suggestions.range.end)
        out.writeVarInt(suggestions.list.size)
        suggestions.list.forEach { suggestion ->
            out.writeString(suggestion.text)
            out.writeBoolean(suggestion.tooltip != null)
            suggestion.tooltip?.let { out.writeText(it.unwrap()) }
        }
    }
}
