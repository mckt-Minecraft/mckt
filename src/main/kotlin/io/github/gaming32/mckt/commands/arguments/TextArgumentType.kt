package io.github.gaming32.mckt.commands.arguments

import com.google.gson.stream.JsonReader
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import io.github.gaming32.mckt.commands.CommandSource
import io.github.gaming32.mckt.commands.textMessage
import io.github.gaming32.mckt.commands.wrap
import io.github.gaming32.mckt.toText
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import java.io.StringReader as IOStringReader

object TextArgumentType : ArgumentType<Component> {
    private val EXAMPLES = listOf("\"hello world\"", "\"\"", "\"{\"text\":\"hello world\"}", "[\"\"]")
    val INVALID_COMPONENT_EXCEPTION = DynamicCommandExceptionType { text ->
        Component.translatable("argument.component.invalid", text.toText()).wrap()
    }

    private val JSON_READER_POS = JsonReader::class.java.getDeclaredField("pos").apply { isAccessible = true }
    private val JSON_READER_LINE_START = JsonReader::class.java.getDeclaredField("lineStart").apply {
        isAccessible = true
    }

    override fun parse(reader: StringReader): Component {
        try {
            val jsonReader = JsonReader(IOStringReader(reader.remaining))
            jsonReader.isLenient = false
            val result = GsonComponentSerializer.gson().serializer().getAdapter(Component::class.java).read(jsonReader)
            reader.cursor += jsonReader.position
            return result ?: throw INVALID_COMPONENT_EXCEPTION.createWithContext(reader, "empty")
        } catch (e: Exception) {
            throw INVALID_COMPONENT_EXCEPTION.createWithContext(reader, e.cause?.textMessage ?: e.textMessage)
        }
    }

    override fun getExamples() = EXAMPLES

    private val JsonReader.position get() = JSON_READER_POS.getInt(this) - JSON_READER_LINE_START.getInt(this) + 1

    fun CommandContext<CommandSource>.getTextComponent(name: String) = getArgument(name, Component::class.java)!!
}
