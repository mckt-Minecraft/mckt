package io.github.gaming32.mckt.packet.play.s2c

import io.github.gaming32.mckt.data.*
import io.github.gaming32.mckt.objects.Identifier
import io.github.gaming32.mckt.objects.Vector3d
import io.github.gaming32.mckt.packet.Packet
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.OutputStream
import kotlin.math.roundToInt
import kotlin.random.Random

@OptIn(ExperimentalSerializationApi::class)
val SOUND_EVENTS = SoundCategory::class.java.getResourceAsStream("/dataexport/soundEvents.json")?.use {
    Json.decodeFromStream<Map<Identifier, Int>>(it)
} ?: mapOf()

enum class SoundCategory {
    MASTER, MUSIC, RECORD, WEATHER, BLOCK, HOSTILE, NEUTRAL, PLAYER, AMBIENT, VOICE
}

sealed class PlaySoundPacket(
    type: Int,
    val category: SoundCategory,
    val position: Vector3d,
    val volume: Float,
    val pitch: Float,
    val seed: Long
) : Packet(type) {
    protected fun writeBase(out: OutputStream) {
        out.writeEnum(category)
        out.writeInt((position.x * 8).roundToInt())
        out.writeInt((position.y * 8).roundToInt())
        out.writeInt((position.z * 8).roundToInt())
        out.writeFloat(volume)
        out.writeFloat(pitch)
        out.writeLong(seed)
    }
}

class PlayBuiltinSoundPacket(
    val id: Identifier,
    category: SoundCategory,
    position: Vector3d,
    volume: Float,
    pitch: Float,
    seed: Long = Random.nextLong()
) : PlaySoundPacket(TYPE, category, position, volume, pitch, seed) {
    companion object {
        const val TYPE = 0x60
    }

    override fun write(out: OutputStream) {
        out.writeVarInt(SOUND_EVENTS[id]
            ?: throw IllegalArgumentException("Sound $id is not builtin")
        )
        writeBase(out)
    }
}

class PlayCustomSoundPacket(
    val id: Identifier,
    category: SoundCategory,
    position: Vector3d,
    volume: Float,
    pitch: Float,
    seed: Long = Random.nextLong()
) : PlaySoundPacket(TYPE, category, position, volume, pitch, seed) {
    companion object {
        const val TYPE = 0x17
    }

    override fun write(out: OutputStream) {
        out.writeIdentifier(id)
        writeBase(out)
    }
}

fun PlaySoundPacket(
    id: Identifier,
    category: SoundCategory,
    position: Vector3d,
    volume: Float,
    pitch: Float,
    seed: Long = Random.nextLong()
) = if (id in SOUND_EVENTS) {
    PlayBuiltinSoundPacket(id, category, position, volume, pitch, seed)
} else {
    PlayCustomSoundPacket(id, category, position, volume, pitch, seed)
}
