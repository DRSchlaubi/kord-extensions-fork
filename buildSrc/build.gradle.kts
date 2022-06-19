import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
    kotlin("jvm") version "1.7.0"
    kotlin("plugin.serialization") version "1.7.0"
}

repositories {
    google()
    gradlePluginPortal()
}

dependencies {
    implementation(kotlin("gradle-plugin"))
    implementation(kotlin("serialization"))

    implementation("gradle.plugin.org.cadixdev.gradle", "licenser", "0.6.1")
    implementation("com.github.jakemarsden", "git-hooks-gradle-plugin", "0.0.2")
    implementation("com.google.devtools.ksp", "com.google.devtools.ksp.gradle.plugin", "1.7.0-1.0.6")
    implementation("io.gitlab.arturbosch.detekt", "detekt-gradle-plugin", "1.20.0")
    implementation("org.jetbrains.dokka", "dokka-gradle-plugin", "1.6.21")

    implementation(gradleApi())
    implementation(localGroovy())
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions {
            languageVersion = "1.5" // Gradle buildscripts still use 1.5
        }
    }
}
