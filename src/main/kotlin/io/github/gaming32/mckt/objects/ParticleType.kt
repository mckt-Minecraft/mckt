package io.github.gaming32.mckt.objects

import io.github.gaming32.mckt.data.Writable
import io.github.gaming32.mckt.data.writeVarInt
import java.io.OutputStream

data class ParticleType(val id: Int, val serializer: OutputStream.() -> Unit = {}) : Writable {
    companion object {
        val FIREWORK = ParticleType(26)
    }

    override fun write(out: OutputStream) {
        out.writeVarInt(id)
        out.serializer()
    }
}
