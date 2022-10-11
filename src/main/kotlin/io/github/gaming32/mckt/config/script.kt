package io.github.gaming32.mckt.config

import io.github.gaming32.mckt.sha1
import io.github.gaming32.mckt.toHexString
import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.compilationCache
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.CompiledScriptJarsCache

object ConfigCompilationConfiguration : ScriptCompilationConfiguration({
    baseClass(ServerConfig::class)
    jvm {
        defaultImports(
            "io.github.gaming32.mckt.*",
            "io.github.gaming32.mckt.config.frontendapi.*",
            "net.kyori.adventure.extra.kotlin.*",
            "net.kyori.adventure.util.*",
            "net.kyori.adventure.text.*",
            "net.kyori.adventure.text.Component.*",
            "net.kyori.adventure.text.format.*",
            "net.kyori.adventure.text.format.TextColor.color",
        )
        dependenciesFromCurrentContext(wholeClasspath = true)
    }
    ide {
        acceptedLocations(ScriptAcceptedLocation.Everywhere)
    }
    hostConfiguration(ScriptingHostConfiguration(defaultJvmScriptingHostConfiguration) {
        jvm {
            compilationCache(CompiledScriptJarsCache { source, _ ->
                File("cache/config/${source.text.encodeToByteArray().sha1().toHexString()}.jar").also {
                    it.parentFile.mkdirs()
                }
            })
        }
    })
})

object ConfigEvaluationConfiguration : ScriptEvaluationConfiguration({
    compilationConfiguration(ConfigCompilationConfiguration)
})

object ConfigScriptingHost : BasicJvmScriptingHost() {
    suspend fun evalSuspend(script: SourceCode) =
        compiler(script, ConfigCompilationConfiguration).onSuccess {
            evaluator(it, ConfigEvaluationConfiguration)
        }
}

suspend fun evalConfigFile(configFile: File) = ConfigScriptingHost
    .evalSuspend(configFile.toScriptSource())
    .valueOr { failure ->
        throw ConfigErrorException(failure.reports
            .filter(ScriptDiagnostic::isError)
            .joinToString("\n") { it.render().replace("\n", "\n    ") }
        )
    }
    .returnValue
    .scriptInstance as ServerConfig
