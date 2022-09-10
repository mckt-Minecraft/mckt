@file:UseSerializers(TextSerializer::class)

package io.github.gaming32.mckt

import io.github.gaming32.mckt.objects.TextSerializer
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import net.benwoodworth.knbt.decodeFromStream
import net.benwoodworth.knbt.encodeToStream
import net.kyori.adventure.text.Component
import java.io.InputStream
import java.io.OutputStream
import kotlin.reflect.KType

@Serializable
enum class SaveFormat(val fileExtension: String) {
    @SerialName("nbt") NBT(".nbt") {
        @Suppress("UNCHECKED_CAST")
        override fun <T> decodeFromStream(input: InputStream, type: KType) =
            SAVE_NBT.decodeFromStream(SAVE_NBT.serializersModule.serializer(type), input) as T

        override fun <T> encodeToStream(value: T, output: OutputStream, type: KType) =
            SAVE_NBT.encodeToStream(SAVE_NBT.serializersModule.serializer(type), value, output)
    },
    @OptIn(ExperimentalSerializationApi::class)
    @SerialName("json") JSON(".json") {
        @Suppress("UNCHECKED_CAST")
        override fun <T> decodeFromStream(input: InputStream, type: KType) =
            Json.decodeFromStream(Json.serializersModule.serializer(type), input) as T

        override fun <T> encodeToStream(value: T, output: OutputStream, type: KType) {
            Json.encodeToStream(Json.serializersModule.serializer(type), value, output)
        }
    };

    abstract fun <T> decodeFromStream(input: InputStream, type: KType): T
    abstract fun <T> encodeToStream(value: T, output: OutputStream, type: KType)
}

@Serializable
class ServerConfig(
    var viewDistance: Int = 10,
    var simulationDistance: Int = 10,
    var maxPlayers: Int = 20,
    var seed: Long? = null,
    var motd: Component = Component.text("My mckt server"),
    var defaultWorldGenerator: WorldGenerator = WorldGenerator.NORMAL,
    var defaultSaveFormat: SaveFormat = SaveFormat.NBT,
    var networkCompressionThreshold: Int = 256,
    var autosavePeriod: Int = 6000
)
