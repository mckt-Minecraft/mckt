package io.github.gaming32.mckt.packet.play.s2c

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.tree.ArgumentCommandNode
import com.mojang.brigadier.tree.CommandNode
import com.mojang.brigadier.tree.LiteralCommandNode
import com.mojang.brigadier.tree.RootCommandNode
import io.github.gaming32.mckt.commands.CommandSource
import io.github.gaming32.mckt.commands.SuggestionProviders.id
import io.github.gaming32.mckt.commands.arguments.ArgumentTypes
import io.github.gaming32.mckt.commands.arguments.ArgumentTypes.networkSerialize
import io.github.gaming32.mckt.commands.arguments.ArgumentTypes.typeId
import io.github.gaming32.mckt.data.*
import io.github.gaming32.mckt.objects.Identifier
import io.github.gaming32.mckt.packet.Packet
import java.io.OutputStream

class CommandTreePacket(rootNode: RootCommandNode<CommandSource>) : Packet(TYPE) {
    companion object {
        const val TYPE = 0x0F
        private const val TYPE_MASK = 0x03
        private const val TYPE_ROOT = 0
        private const val TYPE_LITERAL = 1
        private const val TYPE_ARGUMENT = 2
        private const val FLAG_EXECUTABLE = 0x04
        private const val FLAG_REDIRECT = 0x08
        private const val FLAG_CUSTOM_SUGGESTIONS = 0x10

        private fun traverse(tree: RootCommandNode<CommandSource>): Map<CommandNode<CommandSource>, Int> {
            val result = mutableMapOf<CommandNode<CommandSource>, Int>()
            val queue = ArrayDeque<CommandNode<CommandSource>>(listOf(tree))

            while (queue.isNotEmpty()) {
                val node = queue.removeFirst()
                if (node !in result) {
                    result[node] = result.size
                    queue += node.children
                    node.redirect?.let { queue += it }
                }
            }

            return result
        }

        private fun collect(nodes: Map<CommandNode<CommandSource>, Int>): List<NodeData> {
            val result = arrayOfNulls<NodeData>(nodes.size)
            nodes.forEach { (key, value) ->
                result[value] = key.convert(nodes)
            }
            @Suppress("UNCHECKED_CAST")
            return result.toList() as List<NodeData>
        }

        private fun CommandNode<CommandSource>.convert(nodes: Map<CommandNode<CommandSource>, Int>): NodeData {
            var flags = 0
            val redirectIndex = redirect?.let {
                flags = FLAG_REDIRECT
                nodes[it]
            } ?: 0

            if (command != null) {
                flags = flags or FLAG_EXECUTABLE
            }

            val extraData = when (this) {
                is RootCommandNode -> {
                    flags = flags or TYPE_ROOT
                    null
                }

                is ArgumentCommandNode<CommandSource, *> -> {
                    flags = flags or TYPE_ARGUMENT
                    if (customSuggestions != null) {
                        flags = flags or FLAG_CUSTOM_SUGGESTIONS
                    }
                    ArgumentExtraData(this)
                }

                is LiteralCommandNode -> {
                    flags = flags or TYPE_LITERAL
                    LiteralExtraData(literal)
                }

                else -> throw UnsupportedOperationException("Unknown node type $this")
            }

            return NodeData(
                flags,
                children.asSequence()
                    .map { nodes[it]!! }
                    .toList()
                    .toIntArray(),
                redirectIndex,
                extraData
            )
        }
    }

    class NodeData(
        val flags: Int,
        val children: IntArray,
        val redirectNode: Int,
        val extraData: Writable? = null
    ) : Writable {
        override fun write(out: OutputStream) {
            out.writeByte(flags)
            out.writeVarInt(children.size)
            children.forEach { out.writeVarInt(it) }
            if ((flags and FLAG_REDIRECT) != 0) {
                out.writeVarInt(redirectNode)
            }
            extraData?.write(out)
        }

        override fun toString() =
            "NodeData(flags=$flags, children=${children.contentToString()}, redirectNode=$redirectNode, " +
                "extraData=$extraData)"
    }

    data class ArgumentExtraData(
        val name: String,
        val type: ArgumentType<*>,
        val customSuggestions: Identifier? = null
    ) : Writable {
        constructor(node: ArgumentCommandNode<CommandSource, *>) : this(
            node.name,
            node.type,
            node.customSuggestions?.id
        )

        override fun write(out: OutputStream) {
            out.writeString(name)
            out.writeVarInt(type.typeId?.let { id ->
                ArgumentTypes.getNetworkId(id)
            } ?: throw IllegalArgumentException("Unsupported networked argument type $type"))
            type.networkSerialize(out)
            if (customSuggestions != null) {
                out.writeIdentifier(customSuggestions)
            }
        }
    }

    data class LiteralExtraData(val literal: String) : Writable {
        override fun write(out: OutputStream) = out.writeString(literal)
    }

    val rootNodeId: Int
    val nodesList = traverse(rootNode).let { nodes ->
        rootNodeId = nodes[rootNode]!!
        collect(nodes)
    }

    override fun write(out: OutputStream) {
        out.writeVarInt(nodesList.size)
        nodesList.forEach { it.write(out) }
        out.writeVarInt(rootNodeId)
    }

    override fun toString() = "CommandTreePacket(nodesList=$nodesList, rootNodeId=$rootNodeId)"
}
