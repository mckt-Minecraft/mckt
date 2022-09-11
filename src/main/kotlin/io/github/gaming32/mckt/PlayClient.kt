@file:OptIn(ExperimentalSerializationApi::class)

package io.github.gaming32.mckt

import io.github.gaming32.mckt.commands.ClientCommandSender
import io.github.gaming32.mckt.commands.CommandSender
import io.github.gaming32.mckt.commands.runCommand
import io.github.gaming32.mckt.objects.Identifier
import io.github.gaming32.mckt.packet.Packet
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
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
    internal var ended = false

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
            val spawnPoint = server.world.findSpawnPoint()
            PlayerData(spawnPoint.x + 0.5, spawnPoint.y.toDouble(), spawnPoint.z + 0.5)
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
        syncOpLevel()
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

        sendPacket(SetContainerContentPacket(0u, *data.inventory))
        sendPacket(ClientboundSetHeldItemPacket(data.selectedHotbarSlot))

        val spawnPlayerPacket = SpawnPlayerPacket(entityId, uuid, data.x, data.y, data.z, data.yaw, data.pitch)
        val syncTrackedDataPacket = SyncTrackedDataPacket(entityId, data.flags, data.flying)
        val equipment = data.getEquipment()
        val setEquipmentPacket = if (equipment.isNotEmpty()) {
            SetEquipmentPacket(entityId, *equipment.toList().toTypedArray())
        } else {
            null
        }
        for (client in server.clients.values) {
            client.sendPacket(syncTrackedDataPacket)
            if (client === this@PlayClient) continue
            client.sendPacket(spawnPlayerPacket)
            if (setEquipmentPacket != null) {
                client.sendPacket(setEquipmentPacket)
            }
            sendPacket(SpawnPlayerPacket(
                client.entityId, client.uuid,
                client.data.x, client.data.y, client.data.z,
                client.data.yaw, client.data.pitch
            ))
            sendPacket(SyncTrackedDataPacket(client.entityId, client.data.flags, data.flying))
            val otherEquipment = client.data.getEquipment()
            if (otherEquipment.isNotEmpty()) {
                sendPacket(SetEquipmentPacket(client.entityId, *otherEquipment.toList().toTypedArray()))
            }
        }

        loadChunksAroundPlayer()
    }

    internal suspend fun syncOpLevel() = sendPacket(EntityEventPacket(
        entityId,
        (EntityEvent.PlayerEvent.SET_OP_LEVEL_0 + min(data.operatorLevel.toUInt(), 4u)).toUByte()
    ))

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
                        chunk.x, chunk.z, withContext(chunk.world.networkSerializationPool) {
                            encodeData(chunk::networkEncode)
                        }
                    ))
                }
            }
        }
    } }

    suspend fun handlePackets() {
        while (server.running && !ended) {
            val packet = try {
                readPacket()
            } catch (e: Exception) {
                if (e is ClosedReceiveChannelException) break
                sendPacket(SystemChatPacket(
                    Component.text(e.toString()).color(NamedTextColor.GOLD), true
                ))
                LOGGER.error("Client connection had error", e)
                continue
            }
            try {
                when (packet) {
                    is ConfirmTeleportationPacket -> if (packet.teleportId >= nextTeleportId) {
                        LOGGER.warn("Client sent unknown teleportId {}", packet.teleportId)
                    }

                    is CommandPacket -> commandSender.runCommand(packet.command)
                    is ServerboundChatPacket -> {
                        LOGGER.info("CHAT: <{}> {}", username, packet.message)
                        server.broadcast(
                            SystemChatPacket(
                                Component.translatable(
                                    "chat.type.text",
                                    Component.text(username),
                                    Component.text(packet.message)
                                )
                            )
                        ) { it.options.chatMode == 0 }
                    }

                    is ClientOptionsPacket -> options = packet.options
                    is PlayPluginPacket -> LOGGER.info("Plugin packet {}", packet.channel)
                    is MovementPacket -> {
                        val shouldUseTeleport = packet.x != null && (
                            (packet.x - data.x) * (packet.x - data.x) +
                                (packet.y!! - data.y) * (packet.y - data.y) +
                                (packet.z!! - data.z) * (packet.z - data.z)
                            ) > 64.0
                        if (!shouldUseTeleport) {
                            if (packet.x != null && packet.yaw != null) {
                                server.broadcastExcept(
                                    this, EntityPositionAndRotationUpdatePacket(
                                        entityId,
                                        packet.x - data.x,
                                        packet.y!! - data.y,
                                        packet.z!! - data.z,
                                        packet.yaw,
                                        packet.pitch!!,
                                        packet.onGround
                                    )
                                )
                            } else if (packet.x != null) {
                                server.broadcastExcept(
                                    this, EntityPositionUpdatePacket(
                                        entityId,
                                        packet.x - data.x,
                                        packet.y!! - data.y,
                                        packet.z!! - data.z,
                                        packet.onGround
                                    )
                                )
                            } else if (packet.yaw != null) {
                                server.broadcastExcept(
                                    this, EntityRotationUpdatePacket(
                                        entityId,
                                        packet.yaw,
                                        packet.pitch!!,
                                        packet.onGround
                                    )
                                )
                            }
                        }
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
                        if (shouldUseTeleport) {
                            server.broadcastExcept(
                                this, EntityTeleportPacket(
                                    entityId, data.x, data.y, data.z, data.yaw, data.pitch, data.onGround
                                )
                            )
                        }
                        if (packet.yaw != null) {
                            server.broadcastExcept(this, SetHeadRotationPacket(entityId, data.yaw))
                        }
                        if (data.onGround && data.isFallFlying) {
                            data.isFallFlying = false
                            server.broadcast(SyncTrackedDataPacket(entityId, data.flags, data.flying))
                        }
                    }

                    is ServerboundPlayerAbilitiesPacket -> data.flying = packet.flying
                    is PlayerCommandPacket -> {
                        var syncTracker = false
                        when (packet.action) {
                            PlayerCommandPacket.START_SNEAKING -> {
                                data.isSneaking = true
                                syncTracker = true
                            }

                            PlayerCommandPacket.STOP_SNEAKING -> {
                                data.isSneaking = false
                                syncTracker = true
                            }

                            PlayerCommandPacket.START_SPRINTING -> {
                                data.isSprinting = true
                                syncTracker = true
                            }

                            PlayerCommandPacket.STOP_SPRINTING -> {
                                data.isSprinting = false
                                syncTracker = true
                            }

                            PlayerCommandPacket.START_FALL_FLYING -> {
                                data.isFallFlying = true
                                syncTracker = true
                            }

                            else -> LOGGER.warn(
                                "Unsupported PlayerCommandPacket action: 0x{}",
                                packet.action.toString(16)
                            )
                        }
                        if (syncTracker) {
                            server.broadcast(SyncTrackedDataPacket(entityId, data.flags, data.flying))
                        }
                    }

                    is PlayPingPacket -> if (packet.id == pingId) {
                        val pingTime = System.nanoTime() - pingStart
                        pingId = -1
                        server.broadcast(
                            PlayerListUpdatePacket(
                                PlayerListUpdatePacket.UpdatePing(
                                    uuid,
                                    pingTime.nanoseconds.inWholeMilliseconds.toInt()
                                )
                            )
                        )
                    }

                    is ServerboundSetHeldItemPacket -> {
                        data.selectedHotbarSlot = packet.slot
                        server.broadcastExcept(this, SetEquipmentPacket(
                            entityId, SetEquipmentPacket.Slot.MAIN_HAND to data.inventory[data.selectedInventorySlot]
                        ))
                    }
                    is SetCreativeInventorySlotPacket -> {
                        data.inventory[packet.slot] = packet.item
                        val syncSlot = SetEquipmentPacket.Slot.getSlot(packet.slot)
                        if (
                            syncSlot != null &&
                            (syncSlot != SetEquipmentPacket.Slot.MAIN_HAND || packet.slot == data.selectedInventorySlot)
                        ) {
                            server.broadcastExcept(this, SetEquipmentPacket(entityId, syncSlot to packet.item))
                        }
                    }
                    is SwingArmPacket -> server.broadcastExcept(
                        this, EntityAnimationPacket(
                            entityId,
                            if (packet.offhand) EntityAnimationPacket.SWING_OFFHAND else EntityAnimationPacket.SWING_MAINHAND
                        )
                    )

                    is UseItemOnBlockPacket -> {
                        val placePos = packet.location + packet.face.vector
                        val slot = if (packet.offhand) 45 else data.selectedInventorySlot
                        var itemStack = data.inventory[slot]
                        if (itemStack != null) {
                            sendPacket(AcknowledgeBlockChangePacket(packet.sequence))
                            server.world.setBlock(placePos, itemStack.itemId)
                            server.broadcast(SetBlockPacket(placePos, itemStack.itemId))
                            if (!data.gamemode.defaultAbilities.creativeMode) {
                                if (--itemStack.count == 0) {
                                    itemStack = null
                                    data.inventory[slot] = null
                                }
                            }
                            server.broadcastExcept(this, SetEquipmentPacket(
                                entityId,
                                if (packet.offhand) {
                                    SetEquipmentPacket.Slot.OFFHAND
                                } else {
                                    SetEquipmentPacket.Slot.MAIN_HAND
                                } to itemStack
                            ))
                        }
                    }

                    is PlayerActionPacket -> {
                        val finishedAction = if (data.gamemode.defaultAbilities.creativeMode) {
                            PlayerActionPacket.Action.START_DIGGING
                        } else {
                            PlayerActionPacket.Action.FINISH_DIGGING
                        }
                        if (packet.action == finishedAction) {
                            sendPacket(AcknowledgeBlockChangePacket(packet.sequence))
                            server.world.setBlock(packet.location, null)
                            server.broadcastExcept(
                                this, EntityAnimationPacket(
                                    entityId, EntityAnimationPacket.SWING_MAINHAND
                                )
                            )
                            server.broadcast(SetBlockPacket(packet.location, null))
                        }
                    }

                    else -> LOGGER.warn("Unhandled packet {}", packet)
                }
            } catch (e: Exception) {
                LOGGER.warn("Exception in packet handling", e)
                sendPacket(SystemChatPacket(
                    Component.text(e.toString()).color(NamedTextColor.GOLD), true
                ))
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

    override suspend fun sendPacket(packet: Packet) {
        try {
            super.sendPacket(packet)
        } catch (e: IOException) {
            ended = true
            try {
                super.sendPacket(PlayDisconnectPacket(Component.text(e.toString(), NamedTextColor.RED)))
            } catch (_: IOException) {
            }
            socket.dispose()
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun save() {
        dataFile.outputStream().use { PRETTY_JSON.encodeToStream(data, it) }
    }

    fun close() = save()
}
