import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val applicationMainClassName: String by project
val ktorVersion: String by project
val adventureVersion: String by project
val log4jVersion: String by project
val jlineVersion: String by project

plugins {
    application
    kotlin("jvm") version "1.7.20"
    kotlin("plugin.serialization") version "1.7.20"
    id("io.ktor.plugin") version "2.1.1"
}

group = "io.github.gaming32"
version = "1.0-SNAPSHOT"

application {
    mainClass.set(applicationMainClassName)
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = applicationMainClassName
        attributes["Multi-Release"] = true
        attributes["Specification-Version"] = project.version
    }
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://libraries.minecraft.net")
    }
}

dependencies {
    implementation("io.ktor:ktor-network:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    implementation("net.kyori:adventure-api:$adventureVersion")
    implementation("net.kyori:adventure-extra-kotlin:$adventureVersion")
    implementation("net.kyori:adventure-text-serializer-gson:$adventureVersion")
    implementation("net.kyori:adventure-text-serializer-legacy:$adventureVersion")
    implementation("net.kyori:adventure-text-serializer-plain:$adventureVersion")
    implementation("net.kyori:adventure-nbt:$adventureVersion")

    implementation("com.mojang:brigadier:1.0.18")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.0")

    implementation("it.unimi.dsi:fastutil-core:8.5.9")

    implementation("io.michaelrocks.bimap:bimap:1.1.0")

    implementation("org.jetbrains.kotlin:kotlin-scripting-common:1.7.20")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm:1.7.20")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm-host:1.7.20")
    implementation("org.jetbrains.kotlin:kotlin-scripting-intellij:1.7.20")

    implementation("org.apache.logging.log4j:log4j-slf4j-impl:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-core:$log4jVersion")

    implementation("org.jline:jline-reader:$jlineVersion")
    implementation("org.jline:jline-terminal-jansi:$jlineVersion")

    implementation(kotlin("reflect"))
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
