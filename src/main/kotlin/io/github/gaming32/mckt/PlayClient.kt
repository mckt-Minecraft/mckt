@file:OptIn(ExperimentalSerializationApi::class)

package io.github.gaming32.mckt

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.tree.CommandNode
import com.mojang.brigadier.tree.RootCommandNode
import io.github.gaming32.mckt.commands.ClientCommandSource
import io.github.gaming32.mckt.commands.CommandSource
import io.github.gaming32.mckt.commands.SuggestionProviders.localProvider
import io.github.gaming32.mckt.commands.runCommand
import io.github.gaming32.mckt.data.encodeData
import io.github.gaming32.mckt.data.writeString
import io.github.gaming32.mckt.items.ItemEventHandler
import io.github.gaming32.mckt.objects.*
import io.github.gaming32.mckt.packet.Packet
import io.github.gaming32.mckt.packet.PacketState
import io.github.gaming32.mckt.packet.login.c2s.LoginStartPacket
import io.github.gaming32.mckt.packet.login.s2c.LoginDisconnectPacket
import io.github.gaming32.mckt.packet.login.s2c.LoginSuccessPacket
import io.github.gaming32.mckt.packet.login.s2c.SetCompressionPacket
import io.github.gaming32.mckt.packet.play.PlayPingPacket
import io.github.gaming32.mckt.packet.play.PlayPluginPacket
import io.github.gaming32.mckt.packet.play.c2s.*
import io.github.gaming32.mckt.packet.play.s2c.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.future.await
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.*
import kotlin.math.*
import kotlin.time.Duration.Companion.nanoseconds


class PlayClient(
    server: MinecraftServer,
    socket: Socket,
    receiveChannel: ByteReadChannel,
    sendChannel: ByteWriteChannel
) : Client(server, socket, receiveChannel, sendChannel) {
    companion object {
        private val LOGGER = getLogger()
        val STANDING_DIMENSIONS = EntityDimensions(0.6, 1.8)
        val SLEEPING_DIMENSIONS = EntityDimensions(0.2, 0.2)
        val POSE_DIMENSIONS = mapOf(
            EntityPose.STANDING to STANDING_DIMENSIONS,
            EntityPose.SLEEPING to SLEEPING_DIMENSIONS,
            EntityPose.FALL_FLYING to EntityDimensions(0.6, 0.6),
            EntityPose.SWIMMING to EntityDimensions(0.6, 0.6),
            EntityPose.SPIN_ATTACK to EntityDimensions(0.6, 0.6),
            EntityPose.CROUCHING to EntityDimensions(0.6, 1.5),
            EntityPose.DYING to EntityDimensions(0.2, 0.2),
        )
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

    internal lateinit var handlePacketsJob: Job
    private var nextTeleportId = 0

    internal var nextPingId = 0
    internal var pingId = -1
    internal var pingStart = 0L

    lateinit var dataFile: File
        private set
    lateinit var data: PlayerData
        private set
    lateinit var commandSource: CommandSource
        private set
    internal val loadedChunks = mutableSetOf<Pair<Int, Int>>()
    private var ignoreMovementPackets = true

    var options = ClientOptions(this)
    internal var ended = false
    var properties = mapOf<String, Pair<String, String?>>()
        private set

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
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun postHandshake() = coroutineScope {
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
            viewDistance = server.config.viewDistance * 2,
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
        sendPacket(SyncTagsPacket(DEFAULT_TAGS))

        commandSource = ClientCommandSource(this@PlayClient)

        sendPacket(ClientboundPlayerAbilitiesPacket(
            data.gamemode.defaultAbilities.copyCurrentlyFlying(data.flying)
        ))
        syncOpLevel()
        syncPosition(false)

        properties = try {
            val uuid = server.httpClient
                .request("https://api.mojang.com/users/profiles/minecraft/$username")
                .body<JsonObject>()["id"]
                ?.cast<JsonPrimitive>()
                ?.content ?: throw IllegalStateException("No UUID!")
            server.httpClient
                .request("https://sessionserver.mojang.com/session/minecraft/profile/$uuid?unsigned=false")
                .body<JsonObject>()["properties"]
                ?.cast<JsonArray>()
                ?.associate { property ->
                    property as JsonObject
                    property["name"]?.cast<JsonPrimitive>()!!.content to (
                        property["value"]?.cast<JsonPrimitive>()!!.content to
                            property["signature"]?.cast<JsonPrimitive>()?.contentOrNull
                    )
                } ?: throw IllegalStateException("No properties!")
        } catch (e: Exception) {
            LOGGER.warn("Failed to retrieve player skin data", e)
            mapOf()
        }
        sendPacket(PlayerListUpdatePacket(
            *server.clients.values.map { client -> PlayerListUpdatePacket.AddPlayer(
                uuid = client.uuid,
                name = client.username,
                properties = client.properties,
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
                properties = properties,
                gamemode = data.gamemode,
                ping = -1,
                displayName = null,
                signatureData = null
            )
        ))

        sendPacket(SetContainerContentPacket(0u, *data.inventory))
        sendPacket(ClientboundSetHeldItemPacket(data.selectedHotbarSlot))

        val spawnPlayerPacket = SpawnPlayerPacket(entityId, uuid, data.x, data.y, data.z, data.yaw, data.pitch)
        val syncTrackedDataPacket = SyncTrackedDataPacket(
            entityId,
            data.flags,
            data.pose,
            options.displayedSkinParts,
            options.mainHand
        )
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
            sendPacket(SyncTrackedDataPacket(
                client.entityId,
                client.data.flags,
                client.data.pose,
                client.options.displayedSkinParts,
                client.options.mainHand
            ))
            val otherEquipment = client.data.getEquipment()
            if (otherEquipment.isNotEmpty()) {
                sendPacket(SetEquipmentPacket(client.entityId, *otherEquipment.toList().toTypedArray()))
            }
        }

        loadChunksAroundPlayer(3).joinAll()
        syncPosition(false)
        ignoreMovementPackets = false
        loadChunksAroundPlayer()
    }

    suspend fun setOperatorLevel(level: Int) {
        data.operatorLevel = level
        syncOpLevel()
    }

    suspend fun kick(reason: Component) {
        sendPacket(PlayDisconnectPacket(reason))
        ended = true
        socket.dispose()
    }

    private suspend fun syncOpLevel() {
        sendPacket(EntityEventPacket(
            entityId,
            (EntityEvent.PlayerEvent.SET_OP_LEVEL_0 + min(data.operatorLevel.toUInt(), 4u)).toUByte()
        ))
        sendCommandTree()
    }

    internal suspend fun sendCommandTree() {
        val toNetwork = mutableMapOf<CommandNode<CommandSource>, CommandNode<CommandSource>>()
        val rootNode = RootCommandNode<CommandSource>()
        toNetwork[server.commandDispatcher.root] = rootNode
        makeTreeForSource(server.commandDispatcher.root, rootNode, toNetwork)
        sendPacket(CommandTreePacket(rootNode))
    }

    private fun makeTreeForSource(
        tree: CommandNode<CommandSource>,
        result: CommandNode<CommandSource>,
        resultNodes: MutableMap<CommandNode<CommandSource>, CommandNode<CommandSource>>
    ) {
        tree.children.forEach { node ->
            if (node.canUse(commandSource)) {
                val builder = node.createBuilder()
                builder.requires { true }
                if (builder.command != null) {
                    builder.executes { 0 }
                }

                if (builder is RequiredArgumentBuilder<CommandSource, *> && builder.suggestionsProvider != null) {
                    builder.suggests(builder.suggestionsProvider.localProvider)
                }

                if (builder.redirect != null) {
                    builder.redirect(resultNodes[builder.redirect])
                }

                val newNode = builder.build()
                resultNodes[node] = newNode
                result.addChild(newNode)
                if (node.children.isNotEmpty()) {
                    makeTreeForSource(node, newNode, resultNodes)
                }
            }
        }
    }

    private suspend fun loadChunk(x: Int, z: Int) {
        if (loadedChunks.add(x to z)) {
            val chunk = server.world.getChunkOrGenerate(x, z)
            val chunkData = withContext(chunk.world.networkSerializationPool) {
                encodeData(chunk::networkEncode)
            }
            if (sendChannel.isClosedForWrite) return
//            LOGGER.info("{}, {}, {}, {}, {}, {}", x, z, chunk.x, chunk.z, chunk.xInRegion, chunk.zInRegion)
            sendPacket(ChunkAndLightDataPacket(chunk.x, chunk.z, chunkData))
        }
    }

    @Suppress("SuspendFunctionOnCoroutineScope")
    internal suspend fun CoroutineScope.loadChunksAroundPlayer(range: Int = server.config.viewDistance * 2): List<Job> {
        val (playerX, playerZ) = chunkPos
        sendPacket(SetCenterChunkPacket(playerX, playerZ))
        val jobs = mutableListOf<Job>()
        spiralLoop(range, range) { x, z ->
            jobs.add(launch { loadChunk(playerX + x, playerZ + z) })
        }
        return jobs
    }

    val chunkPos get() = floor(data.x / 16).toInt() to floor(data.z / 16).toInt()

    val position get() = Vector3d(data.x, data.y, data.z)

    val boundingBox get() = (POSE_DIMENSIONS[data.pose] ?: STANDING_DIMENSIONS).toBox().offset(data.x, data.y, data.z)

    suspend fun handlePackets() = coroutineScope {
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

                    is CommandPacket -> commandSource.runCommand(packet.command, server.commandDispatcher)
                    is ServerboundChatPacket -> {
                        LOGGER.info("CHAT: <{}> {}", username, packet.message)
                        server.broadcast(SystemChatPacket(
                            Component.translatable(
                                "chat.type.text",
                                Component.text(username),
                                Component.text(packet.message)
                            )
                        )) { it.options.chatMode == 0 }
                    }

                    is ClientOptionsPacket -> {
                        val newOptions = packet.options
                        val changedSkinParts = if (options.displayedSkinParts != newOptions.displayedSkinParts) {
                            newOptions.displayedSkinParts
                        } else {
                            null
                        }
                        val changedMainHand = if (options.mainHand != newOptions.mainHand) {
                            newOptions.mainHand
                        } else {
                            null
                        }
                        options = newOptions
                        if (changedSkinParts != null || changedMainHand != null) {
                            server.broadcast(SyncTrackedDataPacket(
                                entityId,
                                displayedSkinParts = changedSkinParts,
                                mainHand = changedMainHand
                            ))
                        }
                    }
                    is CommandCompletionsRequestPacket -> {
                        val reader = StringReader(packet.command)
                        if (reader.canRead() && reader.peek() == '/') {
                            reader.skip()
                        }
                        val parseResults = server.commandDispatcher.parse(reader, commandSource)
                        server.commandDispatcher
                            .getCompletionSuggestions(parseResults)
                            .await()
                            .let { suggestions ->
                                sendPacket(CommandCompletionsResponsePacket(packet.requestId, suggestions))
                            }
                    }

                    is PlayPluginPacket -> LOGGER.info("Plugin packet {}", packet.channel)
                    is MovementPacket -> {
                        if (ignoreMovementPackets) continue
                        val shouldUseTeleport = packet.x != null && (
                            (packet.x - data.x) * (packet.x - data.x) +
                                (packet.y!! - data.y) * (packet.y - data.y) +
                                (packet.z!! - data.z) * (packet.z - data.z)
                            ) > 64.0
                        if (!shouldUseTeleport) {
                            if (packet.x != null && packet.yaw != null) {
                                server.broadcastExcept(
                                    this@PlayClient, EntityPositionAndRotationUpdatePacket(
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
                                    this@PlayClient, EntityPositionUpdatePacket(
                                        entityId,
                                        packet.x - data.x,
                                        packet.y!! - data.y,
                                        packet.z!! - data.z,
                                        packet.onGround
                                    )
                                )
                            } else if (packet.yaw != null) {
                                server.broadcastExcept(
                                    this@PlayClient, EntityRotationUpdatePacket(
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
                                this@PlayClient, EntityTeleportPacket(
                                    entityId, data.x, data.y, data.z, data.yaw, data.pitch, data.onGround
                                )
                            )
                        }
                        if (packet.yaw != null) {
                            server.broadcastExcept(this@PlayClient, SetHeadRotationPacket(entityId, data.yaw))
                        }
                        if (data.onGround && data.isFallFlying) {
                            data.isFallFlying = false
                            data.pose = if (data.isSneaking) EntityPose.CROUCHING else EntityPose.STANDING
                            server.broadcast(SyncTrackedDataPacket(entityId, data.flags, data.pose))
                        }
                    }

                    is ServerboundPlayerAbilitiesPacket -> {
                        data.flying = packet.flying
                        data.isFallFlying = false
                        data.pose = EntityPose.STANDING
                        server.broadcast(SyncTrackedDataPacket(entityId, data.flags, data.pose))
                    }

                    is PlayerCommandPacket -> {
                        var syncTracker = false
                        when (packet.action) {
                            PlayerCommandPacket.START_SNEAKING -> {
                                data.isSneaking = true
                                data.pose = if (data.flying) {
                                    EntityPose.STANDING
                                } else if (data.isFallFlying) {
                                    EntityPose.FALL_FLYING
                                } else {
                                    EntityPose.CROUCHING
                                }
                                syncTracker = true
                            }

                            PlayerCommandPacket.STOP_SNEAKING -> {
                                data.isSneaking = false
                                data.pose = if (data.isFallFlying) {
                                    EntityPose.FALL_FLYING
                                } else {
                                    EntityPose.STANDING
                                }
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
                                data.pose = EntityPose.FALL_FLYING
                                syncTracker = true
                            }

                            else -> LOGGER.warn(
                                "Unsupported PlayerCommandPacket action: 0x{}",
                                packet.action.toString(16)
                            )
                        }
                        if (syncTracker) {
                            server.broadcast(SyncTrackedDataPacket(entityId, data.flags, data.pose))
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
                        server.broadcastExcept(
                            this@PlayClient, SetEquipmentPacket(
                                entityId,
                                EquipmentSlot.MAIN_HAND to data.inventory[data.selectedInventorySlot]
                            )
                        )
                    }

                    is SetCreativeInventorySlotPacket -> {
                        data.inventory[packet.slot] = packet.item
                        sendPacket(SetContainerSlotPacket(0, packet.slot, packet.item))
                        val syncSlot = EquipmentSlot.getSlot(packet.slot)
                        if (
                            syncSlot != null &&
                            (syncSlot != EquipmentSlot.MAIN_HAND || packet.slot == data.selectedInventorySlot)
                        ) {
                            server.broadcastExcept(this@PlayClient, SetEquipmentPacket(entityId, syncSlot to packet.item))
                        }
                    }

                    is SwingArmPacket -> server.broadcastExcept(
                        this@PlayClient, EntityAnimationPacket(
                            entityId,
                            if (packet.offhand) EntityAnimationPacket.SWING_OFFHAND else EntityAnimationPacket.SWING_MAINHAND
                        )
                    )

                    is PlayerActionPacket -> {
                        val finishedAction = if (data.gamemode.defaultAbilities.creativeMode) {
                            PlayerActionPacket.Action.START_DIGGING
                        } else {
                            PlayerActionPacket.Action.FINISH_DIGGING
                        }
                        if (packet.action == finishedAction) {
                            sendPacket(AcknowledgeBlockChangePacket(packet.sequence))
                            server.world.setBlock(packet.location, Blocks.AIR)
                            server.broadcastExcept(this@PlayClient, EntityAnimationPacket(
                                entityId, EntityAnimationPacket.SWING_MAINHAND
                            ))
                            server.broadcast(SetBlockPacket(packet.location, Blocks.AIR))
                        } else if (packet.action == PlayerActionPacket.Action.SWAP_OFFHAND) {
                            val newOffhand = data.inventory[data.selectedInventorySlot]
                            val newMainhand = data.inventory[EquipmentSlot.OFFHAND.rawSlot]
                            data.inventory[data.selectedInventorySlot] = newMainhand
                            data.inventory[EquipmentSlot.OFFHAND.rawSlot] = newOffhand
                            sendPacket(SetContainerSlotPacket(-2, data.selectedInventorySlot, newMainhand))
                            sendPacket(SetContainerSlotPacket(-2, EquipmentSlot.OFFHAND.rawSlot, newOffhand))
                            server.broadcastExcept(this@PlayClient, SetEquipmentPacket(
                                entityId,
                                EquipmentSlot.MAIN_HAND to newMainhand,
                                EquipmentSlot.OFFHAND to newOffhand
                            ))
                        }
                    }

                    is UseItemOnBlockPacket -> {
                        val slot = if (packet.offhand) EquipmentSlot.OFFHAND.rawSlot else data.selectedInventorySlot
                        var itemStack = data.inventory[slot]
                        if (itemStack != null) {
                            sendPacket(AcknowledgeBlockChangePacket(packet.sequence))
                            val eventHandler = server.itemEventHandlers[itemStack.itemId]
                            if (eventHandler != null) {
                                val result = eventHandler.useOnBlock(ItemEventHandler.BlockUseEvent(
                                    itemStack,
                                    this@PlayClient, this@coroutineScope,
                                    packet.offhand, packet.sequence,
                                    packet.location, packet.face,
                                    packet.cursorX, packet.cursorY, packet.cursorZ,
                                    packet.insideBlock
                                ))
                                if (
                                    result == ItemEventHandler.Result.USE_UP &&
                                    !data.gamemode.defaultAbilities.creativeMode
                                ) {
                                    itemStack.count--
                                }
                                if (itemStack.count == 0) {
                                    itemStack = null
                                    data.inventory[slot] = null
                                }
                                server.broadcastExcept(this@PlayClient, SetEquipmentPacket(
                                    entityId,
                                    if (packet.offhand) {
                                        EquipmentSlot.OFFHAND
                                    } else {
                                        EquipmentSlot.MAIN_HAND
                                    } to itemStack
                                ))
                            }
                        }
                    }

                    is UseItemPacket -> {
                        val slot = if (packet.offhand) EquipmentSlot.OFFHAND.rawSlot else data.selectedInventorySlot
                        var itemStack = data.inventory[slot]
                        if (itemStack != null) {
                            val eventHandler = server.itemEventHandlers[itemStack.itemId]
                            if (eventHandler != null) {
                                val result = eventHandler.use(ItemEventHandler.UseEvent(
                                    itemStack,
                                    this@PlayClient, this@coroutineScope,
                                    packet.offhand, packet.sequence
                                ))
                                if (
                                    result == ItemEventHandler.Result.USE_UP &&
                                    !data.gamemode.defaultAbilities.creativeMode
                                ) {
                                    itemStack.count--
                                }
                                if (itemStack.count == 0) {
                                    itemStack = null
                                    data.inventory[slot] = null
                                }
                                server.broadcastExcept(this@PlayClient, SetEquipmentPacket(
                                    entityId,
                                    if (packet.offhand) {
                                        EquipmentSlot.OFFHAND
                                    } else {
                                        EquipmentSlot.MAIN_HAND
                                    } to itemStack
                                ))
                            }
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
    ) = coroutineScope {
        x?.let { data.x = it }
        y?.let { data.y = it }
        z?.let { data.z = it }
        yaw?.let { data.yaw = it }
        pitch?.let { data.pitch = it }
        syncPosition(true)
        if (x != null || z != null) {
            ignoreMovementPackets = true
            loadChunksAroundPlayer(3).joinAll()
            syncPosition(false)
            ignoreMovementPackets = false
            loadChunksAroundPlayer()
        }
    }

    val rotationVector: Vector3d get() {
        val f = data.pitch * (Math.PI / 180.0).toFloat()
        val g = -data.yaw * (Math.PI / 180.0).toFloat()
        val h = cos(g)
        val i = sin(g)
        val j = cos(f)
        val k = sin(f)
        return Vector3d((i * j).toDouble(), (-k).toDouble(), (h * j).toDouble())
    }

    suspend fun sendChat(text: Component) = sendPacket(SystemChatPacket(text))

    internal suspend fun syncPosition(toOthers: Boolean, toSelf: Boolean = true) {
        if (toSelf) {
            sendPacket(PlayerPositionSyncPacket(
                nextTeleportId++,
                data.x,
                data.y,
                data.z,
                data.yaw,
                data.pitch
            ))
        }
        if (toOthers) {
            server.broadcastExcept(this@PlayClient, EntityTeleportPacket(
                entityId,
                data.x,
                data.y,
                data.z,
                data.yaw,
                data.pitch,
                data.onGround
            ))
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
        if (new == Gamemode.SPECTATOR) {
            data.flags = data.flags or EntityFlags.INVISIBLE
        } else {
            data.flags = data.flags and EntityFlags.INVISIBLE.inv()
        }
        server.broadcast(SyncTrackedDataPacket(entityId, data.flags))
    }

    override suspend fun sendPacket(packet: Packet) {
        if (socket.isClosed) {
            if (LOGGER.isDebugEnabled) {
                LOGGER.warn("Attempted to write to closed socket", Throwable())
            } else {
                LOGGER.warn("Attempted to write to closed socket")
            }
            return
        }
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
