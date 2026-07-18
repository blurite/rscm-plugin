import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.jvm.tasks.Jar

plugins {
    kotlin("jvm") version "2.4.0"
    `java-library`
    `maven-publish`
}

group = "io.blurite"
version = "2026.2"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":rscm-core"))
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.4.0")

    testImplementation(kotlin("test-junit5"))
    testImplementation(project(":rscm-annotations"))
    testImplementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.4.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        optIn.add("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
    }
}

val compilerPluginJar = tasks.named<Jar>("jar")

tasks.test {
    dependsOn(compilerPluginJar)
    useJUnitPlatform()
    doFirst {
        systemProperty(
            "rscm.compiler.plugin.jar",
            compilerPluginJar.get().archiveFile.get().asFile.absolutePath,
        )
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            name = "test"
            url = rootProject.layout.buildDirectory.dir("test-maven-repository").get().asFile.toURI()
        }
    }
}
