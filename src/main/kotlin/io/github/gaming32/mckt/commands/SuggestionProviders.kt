package io.github.gaming32.mckt.commands

import com.mojang.brigadier.suggestion.SuggestionProvider
import io.github.gaming32.mckt.objects.Identifier

object SuggestionProviders {
    private val REGISTRY = mutableMapOf<Identifier, SuggestionProvider<CommandSender>>()
    private val ASK_SERVER_ID = Identifier("ask_server")

    val ASK_SERVER = register(ASK_SERVER_ID) { ctx, _ -> ctx.source.getCompletions(ctx) }

    private class LocalProvider(
        val id: Identifier,
        val inner: SuggestionProvider<CommandSender>
    ) : SuggestionProvider<CommandSender> by inner

    fun register(id: Identifier, provider: SuggestionProvider<CommandSender>): SuggestionProvider<CommandSender> {
        if (id in REGISTRY) {
            throw IllegalArgumentException("A command suggestion provider is already registered with the name $id")
        }
        REGISTRY[id] = provider
        return LocalProvider(id, provider)
    }

    operator fun get(id: Identifier) = REGISTRY[id] ?: ASK_SERVER

    val SuggestionProvider<CommandSender>.id get() = if (this is LocalProvider) id else ASK_SERVER_ID

    val SuggestionProvider<CommandSender>.localProvider get() = this as? LocalProvider ?: ASK_SERVER
}
