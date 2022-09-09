@file:OptIn(ExperimentalSerializationApi::class)

package io.github.gaming32.mckt

import io.github.gaming32.mckt.commands.ClientCommandSender
import io.github.gaming32.mckt.commands.CommandSender
import io.github.gaming32.mckt.commands.runCommand
import io.github.gaming32.mckt.objects.Identifier
import io.github.gaming32.mckt.packet.PacketState
import io.github.gaming32.mckt.packet.encodeData
import io.github.gaming32.mckt.packet.login.c2s.LoginStartPacket
import io.github.gaming32.mckt.packet.login.s2c.LoginDisconnectPacket
import io.github.gaming32.mckt.packet.login.s2c.LoginSuccessPacket
import io.github.gaming32.mckt.packet.login.s2c.SetCompressionPacket
import io.github.gaming32.mckt.packet.play.PlayPingPacket
import io.github.gaming32.mckt.packet.play.PlayPluginPacket
import io.github.gaming32.mckt.packet.play.c2s.*
import io.github.gaming32.mckt.packet.play.s2c.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import java.io.File
import java.io.FileNotFoundException
import java.util.*
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.nanoseconds

class PlayClient(
    server: MinecraftServer,
    socket: Socket,
    receiveChannel: ByteReadChannel,
    sendChannel: ByteWriteChannel
) : Client(server, socket, receiveChannel, sendChannel) {
    companion object {
        private val LOGGER = getLogger()
    }

    data class ClientOptions(
        val locale: String = "en_us",
        val viewDistance: Int = 10,
        val chatMode: Int = 0,
        val chatColors: Boolean = true,
        val displayedSkinParts: Int = 127,
        val mainHand: Int = 0,
        val textFiltering: Boolean = false,
        val allowServerListings: Boolean = true
    ) {
        constructor(client: PlayClient) : this(
            viewDistance = client.server.config.viewDistance
        )
    }

    override val primaryState = PacketState.PLAY

    lateinit var username: String
        private set
    lateinit var uuid: UUID
        private set
    val entityId = server.nextEntityId++

    lateinit var handlePacketsJob: Job
    internal var nextTeleportId = 0

    internal var nextPingId = 0
    internal var pingId = -1
    internal var pingStart = 0L

    lateinit var dataFile: File
        private set
    lateinit var data: PlayerData
        private set
    lateinit var commandSender: CommandSender
        private set
    private val loadedChunks = mutableSetOf<Pair<Int, Int>>()

    var options = ClientOptions(this)

    suspend fun handshake() {
        val loginStart = PacketState.LOGIN.readPacket<LoginStartPacket>(receiveChannel, false)
        if (loginStart == null) {
            sendPacket(LoginDisconnectPacket(Component.text("Unexpected packet")))
            socket.dispose()
            return
        }
        username = loginStart.username
        if (!(username matches USERNAME_REGEX)) {
            sendPacket(
                LoginDisconnectPacket(Component.text("Username doesn't match regex $USERNAME_REGEX"))
            )
            socket.dispose()
            return
        }
        uuid = UUID.nameUUIDFromBytes("OfflinePlayer:$username".encodeToByteArray())
        val useCompression = server.config.networkCompressionThreshold
        if (useCompression != -1) {
            sendPacket(SetCompressionPacket(useCompression))
            compression = useCompression
        }
        sendPacket(LoginSuccessPacket(uuid, username))
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun postHandshake() = coroutineScope {
        dataFile = File(server.world.playersDir, "$username.json")
        data = try {
            dataFile.inputStream().use { PRETTY_JSON.decodeFromStream(it) }
        } catch (e: Exception) {
            if (e !is FileNotFoundException) {
                LOGGER.warn("Couldn't read player data, creating anew", e)
            }
            PlayerData()
        }

        sendPacket(PlayLoginPacket(
            entityId = entityId,
            hardcore = false,
            gamemode = data.gamemode,
            previousGamemode = null,
            dimensions = listOf(Identifier("overworld")),
            registryCodec = DEFAULT_REGISTRY_CODEC,
            dimensionType = Identifier("overworld"),
            dimensionName = Identifier("overworld"),
            hashedSeed = 0L,
            maxPlayers = server.config.maxPlayers,
            viewDistance = server.config.viewDistance,
            simulationDistance = server.config.simulationDistance,
            reducedDebugInfo = false,
            enableRespawnScreen = false,
            isDebug = false,
            isFlat = true,
            deathLocation = null
        ))
        sendPacket(PlayPluginPacket(Identifier("brand")) {
            writeString("mckt")
        })

        commandSender = ClientCommandSender(this@PlayClient)

        sendPacket(ClientboundPlayerAbilitiesPacket(
            data.gamemode.defaultAbilities.copyCurrentlyFlying(data.flying)
        ))
        sendPacket(EntityEventPacket(
            entityId,
            (EntityEvent.PlayerEvent.SET_OP_LEVEL_0 + min(data.operatorLevel.toUInt(), 4u)).toUByte()
        ))
        sendPacket(PlayerPositionSyncPacket(nextTeleportId++, data.x, data.y, data.z, data.yaw, data.pitch))
        sendPacket(PlayerListUpdatePacket(
            *server.clients.values.map { client -> PlayerListUpdatePacket.AddPlayer(
                uuid = client.uuid,
                name = client.username,
                properties = mapOf(),
                gamemode = client.data.gamemode,
                ping = -1,
                displayName = null,
                signatureData = null
            ) }.toTypedArray()
        ))
        server.broadcastExcept(this@PlayClient, PlayerListUpdatePacket(
            PlayerListUpdatePacket.AddPlayer(
                uuid = uuid,
                name = username,
                properties = mapOf(),
                gamemode = data.gamemode,
                ping = -1,
                displayName = null,
                signatureData = null
            )
        ))
        val spawnPlayerPacket = SpawnPlayerPacket(entityId, uuid, data.x, data.y, data.z, data.yaw, data.pitch)
        for (client in server.clients.values) {
            if (client === this@PlayClient) continue
            client.sendPacket(spawnPlayerPacket)
            sendPacket(SpawnPlayerPacket(
                client.entityId, client.uuid,
                client.data.x, client.data.y, client.data.z,
                client.data.yaw, client.data.pitch
            ))
        }

        delay(10)
        loadChunksAroundPlayer()
    }

    private suspend fun loadChunksAroundPlayer() = coroutineScope { launch {
        val playerX = floor(data.x / 16).toInt()
        val playerZ = floor(data.z / 16).toInt()
        sendPacket(SetCenterChunkPacket(playerX, playerZ))
        spiralLoop(server.config.viewDistance * 2, server.config.viewDistance * 2) { x, z ->
            val absX = playerX + x
            val absZ = playerZ + z
            if (loadedChunks.add(absX to absZ)) {
                launch loadChunk@ {
                    val chunk = server.world.getChunkOrGenerate(absX, absZ)
                    if (sendChannel.isClosedForWrite) return@loadChunk
                    sendPacket(ChunkAndLightDataPacket(
                        chunk.x, chunk.z, chunk.heightmap, encodeData(chunk::networkEncode)
                    ))
                }
            }
        }
    } }

    suspend fun handlePackets() {
        while (server.running) {
            val packet = try {
                readPacket()
            } catch (e: Exception) {
                if (e is ClosedReceiveChannelException) break
                sendPacket(SystemChatPacket(
                    Component.text(e.toString()).color(NamedTextColor.GOLD), true
                ))
                LOGGER.warn("Client connection had error", e)
                continue
            }
            when (packet) {
                is ConfirmTeleportationPacket -> if (packet.teleportId >= nextTeleportId) {
                    LOGGER.warn("Client sent unknown teleportId {}", packet.teleportId)
                }
                is CommandPacket -> commandSender.runCommand(packet.command)
                is ServerboundChatPacket -> server.broadcast(SystemChatPacket(
                    Component.translatable(
                        "chat.type.text",
                        Component.text(username),
                        Component.text(packet.message)
                    )
                )) { it.options.chatMode == 0 }
                is ClientOptionsPacket -> options = packet.options
                is PlayPluginPacket -> LOGGER.info("Plugin packet {}", packet.channel)
                is MovementPacket -> {
//                    if (packet.x != null && packet.yaw != null) {
//                        server.broadcastExcept(this@PlayClient, EntityPositionAndRotationPacket(
//                            entityId, data.x, data.y, data.z, data.yaw, data.pitch, data.onGround
//                        ))
//                    } else if (packet.x != null) {
//                        server.broadcastExcept(this@PlayClient, EntityPositionPacket(
//                            entityId, data.x, data.y, data.z, data.onGround
//                        ))
//                    } else if (packet.yaw != null) {
//                        server.broadcastExcept(this@PlayClient, EntityRotationPacket(
//                            entityId, data.yaw, data.pitch, data.onGround
//                        ))
//                    }
                    if (packet.x != null) {
                        if (
                            data.z.roundToInt() / 16 != packet.z!!.roundToInt() / 16 ||
                            data.x.roundToInt() / 16 != packet.x.roundToInt() / 16
                        ) {
                            loadChunksAroundPlayer()
                        }
                        data.x = packet.x
                        data.y = packet.y!!
                        data.z = packet.z
                    }
                    if (packet.yaw != null) {
                        data.yaw = packet.yaw
                        data.pitch = packet.pitch!!
                    }
                    data.onGround = packet.onGround
                    server.broadcastExcept(this@PlayClient, EntityTeleportPacket(
                        entityId, data.x, data.y, data.z, data.yaw, data.pitch, data.onGround
                    ))
                }
                is ServerboundPlayerAbilitiesPacket -> data.flying = packet.flying
                is PlayPingPacket -> if (packet.id == pingId) {
                    val pingTime = System.nanoTime() - pingStart
                    pingId = -1
                    server.broadcast(PlayerListUpdatePacket(
                        PlayerListUpdatePacket.UpdatePing(uuid, pingTime.nanoseconds.inWholeMilliseconds.toInt())
                    ))
                }
                else -> LOGGER.warn("Unhandled packet {}", packet)
            }
        }
    }

    suspend fun teleport(
        x: Double? = null, y: Double? = null, z: Double? = null,
        yaw: Float? = null, pitch: Float? = null
    ) {
        x?.let { data.x = it }
        y?.let { data.y = it }
        z?.let { data.z = it }
        yaw?.let { data.yaw = it }
        pitch?.let { data.pitch = it }
        sendPacket(PlayerPositionSyncPacket(
            nextTeleportId++,
            data.x,
            data.y,
            data.z,
            data.yaw,
            data.pitch
        ))
        server.broadcastExcept(this, EntityTeleportPacket(
            entityId,
            data.x,
            data.y,
            data.z,
            data.yaw,
            data.pitch,
            data.onGround
        ))
        if (x != null || z != null) {
            loadChunksAroundPlayer()
        }
    }

    suspend fun teleport(to: PlayClient) = teleport(
        to.data.x, to.data.y, to.data.z,
        to.data.yaw, to.data.pitch
    )

    suspend fun setGamemode(new: Gamemode) {
        if (new == data.gamemode) return
        data.gamemode = new
        sendPacket(GameEventPacket(GameEventPacket.SET_GAMEMODE, new.ordinal.toFloat()))
        sendPacket(ClientboundPlayerAbilitiesPacket(
            new.defaultAbilities.copyCurrentlyFlying(data.flying)
        ))
        server.broadcast(PlayerListUpdatePacket(
            PlayerListUpdatePacket.UpdateGamemode(uuid, new)
        ))
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun save() {
        dataFile.outputStream().use { PRETTY_JSON.encodeToStream(data, it) }
    }

    fun close() = save()
}
