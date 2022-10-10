package io.github.gaming32.mckt.config

import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost

object ConfigCompilationConfiguration : ScriptCompilationConfiguration({
    baseClass(ServerConfig::class)
    jvm {
        defaultImports(
            "io.github.gaming32.mckt.*",
            "net.kyori.adventure.extra.kotlin.*",
            "net.kyori.adventure.text.*",
            "net.kyori.adventure.text.format.*",
        )
        dependenciesFromCurrentContext(wholeClasspath = true)
    }
    ide {
        acceptedLocations(ScriptAcceptedLocation.Everywhere)
    }
})

object ConfigEvaluationConfiguration : ScriptEvaluationConfiguration({
    compilationConfiguration(ConfigCompilationConfiguration)
})

fun evalConfigFile(configFile: File) = BasicJvmScriptingHost()
    .eval(configFile.toScriptSource(), ConfigCompilationConfiguration, ConfigEvaluationConfiguration)
    .valueOr { failure ->
        throw ConfigErrorException(failure.reports
            .filter(ScriptDiagnostic::isError)
            .joinToString("\n") { it.render().replace("\n", "\n    ") }
        )
    }
    .returnValue
    .scriptInstance as ServerConfig
