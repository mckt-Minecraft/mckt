package io.github.gaming32.mckt.packet.play.c2s

import io.github.gaming32.mckt.packet.MinecraftInputStream
import io.github.gaming32.mckt.packet.MinecraftOutputStream
import io.github.gaming32.mckt.packet.Packet

sealed class MovementPacket(
    type: Int,
    val x: Double? = null,
    val y: Double? = null,
    val z: Double? = null,
    val yaw: Float? = null,
    val pitch: Float? = null,
    val onGround: Boolean
) : Packet(type) {
    override fun write(out: MinecraftOutputStream) {
        x?.let { out.writeDouble(it) }
        y?.let { out.writeDouble(it) }
        z?.let { out.writeDouble(it) }
        yaw?.let { out.writeFloat(it) }
        pitch?.let { out.writeFloat(it) }
        out.writeBoolean(onGround)
    }
}

class PlayerPositionPacket(
    x: Double,
    y: Double,
    z: Double,
    onGround: Boolean
) : MovementPacket(TYPE, x, y, z, onGround = onGround) {
    companion object {
        const val TYPE = 0x14
    }

    constructor(inp: MinecraftInputStream) : this(
        inp.readDouble(),
        inp.readDouble(),
        inp.readDouble(),
        inp.readBoolean()
    )
}

class PlayerPositionAndRotationPacket(
    x: Double,
    y: Double,
    z: Double,
    yaw: Float,
    pitch: Float,
    onGround: Boolean
) : MovementPacket(TYPE, x, y, z, yaw, pitch, onGround) {
    companion object {
        const val TYPE = 0x15
    }

    constructor(inp: MinecraftInputStream) : this(
        inp.readDouble(),
        inp.readDouble(),
        inp.readDouble(),
        inp.readFloat(),
        inp.readFloat(),
        inp.readBoolean()
    )
}

class PlayerRotationPacket(
    yaw: Float,
    pitch: Float,
    onGround: Boolean
) : MovementPacket(TYPE, yaw = yaw, pitch = pitch, onGround = onGround) {
    companion object {
        const val TYPE = 0x16
    }

    constructor(inp: MinecraftInputStream) : this(
        inp.readFloat(),
        inp.readFloat(),
        inp.readBoolean()
    )
}

class PlayerOnGroundPacket(
    onGround: Boolean
) : MovementPacket(TYPE, onGround = onGround) {
    companion object {
        const val TYPE = 0x17
    }

    constructor(inp: MinecraftInputStream) : this(inp.readBoolean())
}
