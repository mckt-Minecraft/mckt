package io.github.gaming32.mckt.packet.play.s2c

import io.github.gaming32.mckt.data.writeByte
import io.github.gaming32.mckt.data.writeVarInt
import io.github.gaming32.mckt.objects.EntityPose
import io.github.gaming32.mckt.packet.Packet
import java.io.OutputStream

data class SyncTrackedDataPacket(
    val entityId: Int,
    // Hardcoded mess :)
    val entityFlags: Int? = null,
    val pose: EntityPose? = null,
    val displayedSkinParts: Int? = null,
    val mainHand: Int? = null
) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x50
    }

    override fun write(out: OutputStream) {
        out.writeVarInt(entityId)
        if (entityFlags != null) {
            out.writeByte(0) // Index = Entity.flags
            out.writeVarInt(0) // Type = Byte
            out.writeByte(entityFlags) // Value
        }
        if (pose != null) {
            out.writeByte(6) // Index = Entity.pose
            out.writeVarInt(18) // Type = Pose
            out.writeVarInt(pose.ordinal) // Value
        }
        if (displayedSkinParts != null) {
            out.writeByte(17) // Index = Player.displayedSkinParts
            out.writeVarInt(0) // Type = Byte
            out.writeByte(displayedSkinParts)
        }
        if (mainHand != null) {
            out.writeByte(18)
            out.writeVarInt(0) // Type = Byte
            out.writeByte(mainHand)
        }
        out.writeByte(0xff) // Index = End
    }
}
