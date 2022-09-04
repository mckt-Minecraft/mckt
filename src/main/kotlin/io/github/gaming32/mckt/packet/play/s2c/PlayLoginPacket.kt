package io.github.gaming32.mckt.packet.play.s2c

import io.github.gaming32.mckt.Gamemode
import io.github.gaming32.mckt.objects.BlockPosition
import io.github.gaming32.mckt.objects.Identifier
import io.github.gaming32.mckt.packet.MinecraftOutputStream
import io.github.gaming32.mckt.packet.Packet
import net.benwoodworth.knbt.NbtCompound

class PlayLoginPacket(
    val entityId: Int,
    val hardcore: Boolean,
    val gamemode: Gamemode,
    val previousGamemode: Gamemode?,
    val dimensions: List<Identifier>,
    val registryCodec: NbtCompound,
    val dimensionType: Identifier,
    val dimensionName: Identifier,
    val hashedSeed: Long,
    val maxPlayers: Int,
    val viewDistance: Int,
    val simulationDistance: Int,
    val reducedDebugInfo: Boolean,
    val enableRespawnScreen: Boolean,
    val isDebug: Boolean,
    val isFlat: Boolean,
    val deathLocation: Pair<Identifier, BlockPosition>?
) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x25
    }

    override fun write(out: MinecraftOutputStream) {
        out.writeInt(entityId)
        out.writeBoolean(hardcore)
        out.writeByte(gamemode.ordinal)
        out.writeByte(previousGamemode?.ordinal ?: -1)
        out.writeVarInt(dimensions.size)
        dimensions.forEach { out.writeIdentifier(it) }
        out.writeNbtTag(registryCodec)
        out.writeIdentifier(dimensionType)
        out.writeIdentifier(dimensionName)
        out.writeLong(hashedSeed)
        out.writeVarInt(maxPlayers)
        out.writeVarInt(viewDistance)
        out.writeVarInt(simulationDistance)
        out.writeBoolean(reducedDebugInfo)
        out.writeBoolean(enableRespawnScreen)
        out.writeBoolean(isDebug)
        out.writeBoolean(isFlat)
        out.writeBoolean(deathLocation != null)
        deathLocation?.let { (dimension, position) ->
            out.writeIdentifier(dimension)
            out.writeBlockPosition(position)
        }
    }
}
