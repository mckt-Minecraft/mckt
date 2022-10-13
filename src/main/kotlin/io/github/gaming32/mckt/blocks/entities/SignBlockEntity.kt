package io.github.gaming32.mckt.blocks.entities

import io.github.gaming32.mckt.nbt.NbtCompound
import io.github.gaming32.mckt.objects.BlockPosition
import io.github.gaming32.mckt.objects.BlockState
import io.github.gaming32.mckt.packet.play.s2c.BlockEntityDataPacket
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import java.util.*

class SignBlockEntity(
    pos: BlockPosition,
    state: BlockState
) : BlockEntity<SignBlockEntity>(SignBlockEntity, pos, state) {
    companion object Type : BlockEntityType<SignBlockEntity>() {
        const val LINES = 4
        private val TEXT_KEYS = arrayOf("Text1", "Text2", "Text3", "Text4")

        override fun create(pos: BlockPosition, state: BlockState) = SignBlockEntity(pos, state)
    }

    val lines = Array<Component>(4) { Component.empty() }
    var editable = true
        set(editable) {
            field = editable
            if (!editable) {
                editor = null
            }
        }
    var editor: UUID? = null
    var color = NamedTextColor.BLACK!!
    var glowingText = false

    override fun writeNbt(nbt: NbtCompound) {
        repeat(LINES) { i ->
            nbt.putString(TEXT_KEYS[i], GsonComponentSerializer.gson().serialize(lines[i]))
        }
        nbt.putString("Color", color.toString())
        nbt.putBoolean("GlowingText", glowingText)
    }

    override fun readNbt(nbt: NbtCompound) {
        editable = false
        color = NamedTextColor.NAMES.value(nbt.getString("Color")) ?: NamedTextColor.BLACK
        repeat(LINES) { i ->
            lines[i] = GsonComponentSerializer.gson().deserialize(nbt.getString(TEXT_KEYS[i]))
        }
        glowingText = nbt.getBoolean("GlowingText")
    }

    override fun updateNetworkSerialize() = BlockEntityDataPacket(this)

    override fun initialNetworkSerialize() = toNbt()

    operator fun get(row: Int) = lines[row]

    operator fun set(row: Int, line: Component) {
        lines[row] = line
    }
}
