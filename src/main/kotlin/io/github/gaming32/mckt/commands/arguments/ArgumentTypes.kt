package io.github.gaming32.mckt.commands.arguments

import com.mojang.brigadier.arguments.*
import io.github.gaming32.mckt.data.MinecraftOutputStream
import io.github.gaming32.mckt.objects.Identifier
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream

@Suppress("BlockingMethodInNonBlockingContext")
object ArgumentTypes {
    private val REGISTRY = mutableMapOf<
        Class<out ArgumentType<*>>,
        Pair<
            Identifier,
            ArgumentType<*>.(out: MinecraftOutputStream) -> Unit
            >
        >()
    @OptIn(ExperimentalSerializationApi::class)
    private val NETWORK_IDS = javaClass.getResourceAsStream("/commandArgumentTypes.json")?.use {
        Json.decodeFromStream<Map<Identifier, Int>>(it)
    } ?: mapOf()

    init {
        register(Identifier("brigadier", "bool"), BoolArgumentType::class.java)
        register(Identifier("brigadier", "float"), FloatArgumentType::class.java) { out ->
            var flags = 0
            if (minimum != Float.MIN_VALUE) {
                flags = 0x01
            }
            if (maximum != Float.MAX_VALUE) {
                flags = flags or 0x02
            }
            out.writeByte(flags)
            if ((flags and 0x01) != 0) {
                out.writeFloat(minimum)
            }
            if ((flags and 0x02) != 0) {
                out.writeFloat(maximum)
            }
        }
        register(Identifier("brigadier", "double"), DoubleArgumentType::class.java) { out ->
            var flags = 0
            if (minimum != Double.MIN_VALUE) {
                flags = 0x01
            }
            if (maximum != Double.MAX_VALUE) {
                flags = flags or 0x02
            }
            out.writeByte(flags)
            if ((flags and 0x01) != 0) {
                out.writeDouble(minimum)
            }
            if ((flags and 0x02) != 0) {
                out.writeDouble(maximum)
            }
        }
        register(Identifier("brigadier", "integer"), IntegerArgumentType::class.java) { out ->
            var flags = 0
            if (minimum != Int.MIN_VALUE) {
                flags = 0x01
            }
            if (maximum != Int.MAX_VALUE) {
                flags = flags or 0x02
            }
            out.writeByte(flags)
            if ((flags and 0x01) != 0) {
                out.writeInt(minimum)
            }
            if ((flags and 0x02) != 0) {
                out.writeInt(maximum)
            }
        }
        register(Identifier("brigadier", "long"), LongArgumentType::class.java) { out ->
            var flags = 0
            if (minimum != Long.MIN_VALUE) {
                flags = 0x01
            }
            if (maximum != Long.MAX_VALUE) {
                flags = flags or 0x02
            }
            out.writeByte(flags)
            if ((flags and 0x01) != 0) {
                out.writeLong(minimum)
            }
            if ((flags and 0x02) != 0) {
                out.writeLong(maximum)
            }
        }
        register(Identifier("brigadier", "string"), StringArgumentType::class.java) { out ->
            out.writeVarInt(type.ordinal)
        }
        register(Identifier("entity"), EntityArgumentType::class.java) { out ->
            var flags = 0
            if (singleTarget) {
                flags = 0x01
            }
            if (playersOnly) {
                flags = flags or 0x02
            }
            out.writeByte(flags)
        }
        register(Identifier("block_pos"), BlockPositionArgumentType::class.java)
        register(Identifier("vec3"), Vector3ArgumentType::class.java)
        register(Identifier("component"), TextArgumentType::class.java)
    }

    fun <T : ArgumentType<*>> register(
        id: Identifier,
        argumentType: Class<T>,
        infoSerializer: T.(out: MinecraftOutputStream) -> Unit = {}
    ) {
        if (argumentType in REGISTRY) {
            throw IllegalArgumentException("Already registered: $id/$argumentType")
        }
        @Suppress("UNCHECKED_CAST")
        REGISTRY[argumentType] = Pair(id, infoSerializer as ArgumentType<*>.(out: MinecraftOutputStream) -> Unit)
    }

    fun getNetworkId(typeId: Identifier) = NETWORK_IDS[typeId]
        ?: throw IllegalArgumentException("Argument type $typeId not supported for network serialization")

    fun ArgumentType<*>.networkSerialize(out: MinecraftOutputStream) {
        val serializer = REGISTRY[javaClass]?.second
            ?: throw IllegalArgumentException("Cannot serialize $this")
        serializer(out)
    }

    val Class<out ArgumentType<*>>.typeId get() = REGISTRY[this]?.first
    val Class<out ArgumentType<*>>.infoSerializer get() = REGISTRY[this]?.second
    val ArgumentType<*>.typeId get() = javaClass.typeId
}
