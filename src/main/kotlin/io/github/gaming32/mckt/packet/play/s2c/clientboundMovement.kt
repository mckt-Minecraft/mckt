package io.github.gaming32.mckt.packet.play.s2c

import io.github.gaming32.mckt.data.MinecraftOutputStream
import io.github.gaming32.mckt.packet.Packet
import kotlin.math.roundToInt

data class EntityTeleportPacket(
    val entityId: Int,
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float,
    val pitch: Float,
    val onGround: Boolean
) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x66
    }

    override fun write(out: MinecraftOutputStream) {
        out.writeVarInt(entityId)
        out.writeDouble(x)
        out.writeDouble(y)
        out.writeDouble(z)
        out.writeDegrees(yaw)
        out.writeDegrees(pitch)
        out.writeBoolean(onGround)
    }
}

sealed class EntityRelativeMovementPacket(
    type: Int,
    val entityId: Int,
    val relX: Double? = null,
    val relY: Double? = null,
    val relZ: Double? = null,
    val yaw: Float? = null,
    val pitch: Float? = null,
    val onGround: Boolean
) : Packet(type) {
    override fun write(out: MinecraftOutputStream) {
        out.writeVarInt(entityId)
        relX?.let { out.writeShort((it * 4096).roundToInt()) }
        relY?.let { out.writeShort((it * 4096).roundToInt()) }
        relZ?.let { out.writeShort((it * 4096).roundToInt()) }
        yaw?.let { out.writeDegrees(it) }
        pitch?.let { out.writeDegrees(it) }
        out.writeBoolean(onGround)
    }
}

class EntityPositionUpdatePacket(
    entityId: Int,
    relX: Double,
    relY: Double,
    relZ: Double,
    onGround: Boolean
) : EntityRelativeMovementPacket(TYPE, entityId, relX, relY, relZ, onGround = onGround) {
    companion object {
        const val TYPE = 0x28
    }

    override fun toString() =
        "EntityPositionUpdatePacket(entityId=$entityId, relX=$relX, relY=$relY, relZ=$relZ, onGround=$onGround)"
}

class EntityPositionAndRotationUpdatePacket(
    entityId: Int,
    relX: Double,
    relY: Double,
    relZ: Double,
    yaw: Float,
    pitch: Float,
    onGround: Boolean
) : EntityRelativeMovementPacket(TYPE, entityId, relX, relY, relZ, yaw, pitch, onGround) {
    companion object {
        const val TYPE = 0x29
    }

    override fun toString() =
        "EntityRelativeMovementPacket(entityId=$entityId, relX=$relX, relY=$relY, relZ=$relZ, yaw=$yaw, pitch=$pitch, " +
            "onGround=$onGround)"
}

class EntityRotationUpdatePacket(
    entityId: Int,
    yaw: Float,
    pitch: Float,
    onGround: Boolean
) : EntityRelativeMovementPacket(TYPE, entityId, yaw = yaw, pitch = pitch, onGround = onGround) {
    companion object {
        const val TYPE = 0x2A
    }

    override fun toString() =
        "EntityRotationUpdatePacket(entityId=$entityId, yaw=$yaw, pitch=$pitch, onGround=$onGround)"
}
