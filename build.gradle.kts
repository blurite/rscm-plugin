import org.jetbrains.intellij.tasks.PatchPluginXmlTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.23"
    java
    idea
    id("org.jetbrains.intellij") version "1.17.4"
}

group = "io.blurite"
version = "1.0"

repositories {
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots/")
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
}

// Add generated language classes source set
sourceSets {
    main {
        java {
            srcDir("src/main/gen")
        }
    }
}

// Intellij/Plugin configuration
intellij {
    version.set("2023.1")
    plugins.set(listOf("java", "Kotlin", "org.toml.lang"))
    pluginName.set("RSCM")
    updateSinceUntilBuild.set(true)
}

tasks.withType<PatchPluginXmlTask> {
    untilBuild.set("")
    changeNotes.set("""Rename files with same name on property rename.""")
}

// Language configuration
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
        apiVersion = "1.8"
        languageVersion = "1.8"
        freeCompilerArgs = listOf("-Xinline-classes")
    }
}
