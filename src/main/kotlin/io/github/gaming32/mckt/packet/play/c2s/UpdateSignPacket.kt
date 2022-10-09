package io.github.gaming32.mckt.packet.play.c2s

import io.github.gaming32.mckt.data.readBlockPosition
import io.github.gaming32.mckt.data.readString
import io.github.gaming32.mckt.data.writeBlockPosition
import io.github.gaming32.mckt.data.writeString
import io.github.gaming32.mckt.objects.BlockPosition
import io.github.gaming32.mckt.packet.Packet
import java.io.InputStream
import java.io.OutputStream

class UpdateSignPacket(
    val location: BlockPosition,
    vararg val lines: String
) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x2E
    }

    init {
        require(lines.size == 4) { "Signs only support exactly 4 lines" }
    }

    constructor(inp: InputStream) : this(
        inp.readBlockPosition(),
        *Array(4) { inp.readString(384) }
    )

    override fun write(out: OutputStream) {
        out.writeBlockPosition(location)
        repeat(4) { i -> out.writeString(lines[i], 384) }
    }
}
