import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val log4jVersion: String by project

plugins {
    application
    kotlin("jvm") version "1.7.10"
    kotlin("plugin.serialization") version "1.7.10"
}

group = "io.github.gaming32"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("io.github.gaming32.mckt.MainKt")
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://libraries.minecraft.net")
    }
}

dependencies {
    implementation("io.ktor:ktor-network:2.1.1")

    implementation("net.kyori:adventure-api:4.11.0")
    implementation("net.kyori:adventure-text-serializer-gson:4.11.0")
    implementation("net.kyori:adventure-text-serializer-legacy:4.11.0")
    implementation("net.kyori:adventure-text-serializer-plain:4.11.0")

    implementation("net.benwoodworth.knbt:knbt:0.11.2")

    implementation("com.mojang:brigadier:1.0.18")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.0")

    implementation("it.unimi.dsi:fastutil:8.5.8")

    implementation("org.apache.logging.log4j:log4j-slf4j-impl:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-core:$log4jVersion")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}