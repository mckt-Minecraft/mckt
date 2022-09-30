package io.github.gaming32.mckt.commands.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import com.mojang.brigadier.context.CommandContext
import io.github.gaming32.mckt.PlayClient
import io.github.gaming32.mckt.commands.CommandSource
import io.github.gaming32.mckt.commands.EntitySelector
import io.github.gaming32.mckt.commands.arguments.*
import io.github.gaming32.mckt.commands.executesSuspend
import io.github.gaming32.mckt.objects.Vector3d
import net.kyori.adventure.text.Component

object TeleportCommand : BuiltinCommand {
    override val helpText = Component.text("Teleport a player")

    override val aliases = listOf("tp")

    override fun buildTree() = literal<CommandSource>("teleport")
        .requires { it.hasPermission(1) }
        .then(argument<CommandSource, EntitySelector>("destination", entity())
            .executesSuspend {
                teleport(listOf(source.entity), getEntity("destination"))
                0
            }
        )
        .then(argument<CommandSource, PositionArgument>("location", Vector3ArgumentType())
            .executesSuspend {
                teleport(listOf(source.entity), getVec3("location"))
                0
            }
        )
        .then(argument<CommandSource, EntitySelector>("target", entities())
            .then(argument<CommandSource, EntitySelector>("destination", entity())
                .executesSuspend {
                    teleport(getEntities("target"), getEntity("destination"))
                    0
                }
            )
            .then(argument<CommandSource, PositionArgument>("location", Vector3ArgumentType())
                .executesSuspend {
                    teleport(getEntities("target"), getVec3("location"))
                    0
                }
            )
        )!!

    suspend fun CommandContext<CommandSource>.teleport(entities: List<PlayClient>, destination: PlayClient) {
        entities.forEach { it.teleport(destination) }
        source.replyBroadcast(
            if (entities.size == 1) {
                Component.translatable(
                    "commands.teleport.success.entity.single",
                    Component.text(entities[0].username),
                    Component.text(destination.username)
                )
            } else {
                Component.translatable(
                    "commands.teleport.success.entity.multiple",
                    Component.text(entities.size),
                    Component.text(destination.username)
                )
            }
        )
    }

    suspend fun CommandContext<CommandSource>.teleport(entities: List<PlayClient>, destination: Vector3d) {
        entities.forEach { it.teleport(destination.x, destination.y, destination.z) }
        source.replyBroadcast(
            if (entities.size == 1) {
                Component.translatable(
                    "commands.teleport.success.location.single",
                    Component.text(entities[0].username),
                    Component.text(destination.x),
                    Component.text(destination.y),
                    Component.text(destination.z)
                )
            } else {
                Component.translatable(
                    "commands.teleport.success.location.multiple",
                    Component.text(entities.size),
                    Component.text(destination.x),
                    Component.text(destination.y),
                    Component.text(destination.z)
                )
            }
        )
    }
}
