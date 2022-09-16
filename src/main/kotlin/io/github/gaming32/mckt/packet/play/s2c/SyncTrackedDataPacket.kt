package io.github.gaming32.mckt.packet.play.s2c

import io.github.gaming32.mckt.objects.EntityPose
import io.github.gaming32.mckt.packet.MinecraftOutputStream
import io.github.gaming32.mckt.packet.Packet

data class SyncTrackedDataPacket(
    val entityId: Int,
    val entityFlags: Int, // Just flags and pose for now
    val pose: EntityPose
) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x50
    }

    override fun write(out: MinecraftOutputStream) {
        out.writeVarInt(entityId)
        out.writeByte(0) // Index 0 = Entity.flags
        out.writeVarInt(0) // Type 0 = Byte
        out.writeByte(entityFlags) // Value 0
        out.writeByte(6) // Index 1 = Entity.pose
        out.writeVarInt(18) // Type 1 = Pose
        out.writeVarInt(pose.ordinal) // Value 1
        out.writeByte(0xff) // Index 2 = End
    }
}
