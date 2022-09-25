package io.github.gaming32.mckt.packet.play.s2c

import io.github.gaming32.mckt.data.*
import io.github.gaming32.mckt.objects.ParticleType
import io.github.gaming32.mckt.objects.Vector3d
import io.github.gaming32.mckt.packet.Packet
import java.io.OutputStream

data class ParticlePacket(
    val particle: ParticleType,
    val longDistance: Boolean,
    val origin: Vector3d,
    val offsetX: Float,
    val offsetY: Float,
    val offsetZ: Float,
    val speed: Float = 1f,
    val count: Int = 0
) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x23
    }

    override fun write(out: OutputStream) {
        out.writeVarInt(particle.id)
        out.writeBoolean(longDistance)
        out.writeVector3d(origin)
        out.writeFloat(offsetX)
        out.writeFloat(offsetY)
        out.writeFloat(offsetZ)
        out.writeFloat(speed)
        out.writeInt(count)
        particle.serializer(out)
    }
}
