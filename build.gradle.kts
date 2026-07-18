
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.gradle.jvm.tasks.Jar

plugins {
    kotlin("jvm") version "2.4.0"
    java
    idea
    id("org.jetbrains.intellij.platform") version "2.7.1"
}

group = "io.blurite"
version = "1.0"

repositories {
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots/")
    gradlePluginPortal()
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

val rscmCompilerBundle by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    rscmCompilerBundle(project(":rscm-annotations"))
    rscmCompilerBundle(project(":rscm-kotlin-compiler"))
    rscmCompilerBundle(project(":rscm-java-compiler"))
    rscmCompilerBundle(project(":rscm-gradle-plugin")) {
        isTransitive = false
    }

    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    testImplementation("junit:junit:4.13.2")
    intellijPlatform {
        intellijIdeaCommunity("2025.2")

        bundledPlugin("org.jetbrains.kotlin")
        bundledPlugin("com.intellij.java")
        bundledPlugin("org.toml.lang")
        testFramework(TestFrameworkType.Platform)
    }
}

tasks.register<Jar>("buildCompilerJar") {
    group = "build"
    description = "Builds the standalone Kotlin and Java RSCM compiler plugin JAR"
    archiveFileName.set("rscm-compiler-$version.jar")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    dependsOn(rscmCompilerBundle)
    from({
        rscmCompilerBundle.map { dependency ->
            if (dependency.isDirectory) dependency else zipTree(dependency)
        }
    })

    exclude("META-INF/*.DSA", "META-INF/*.RSA", "META-INF/*.SF")
}

// Add generated language classes source set
sourceSets {
    main {
        java {
            srcDir("src/main/gen")
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

intellijPlatform {
    pluginConfiguration {
        name = "RSCM"
        version = "2025.2.1"
        changeNotes.set("Add Java and Kotlin compiler validation annotations")
    }

    pluginVerification {
        ides {
            create("IC", "2025.2")
        }

        failureLevel.set(
            setOf(
                // These will make the task fail on common K2-incompatible patterns
                FailureLevel.COMPATIBILITY_PROBLEMS,
                FailureLevel.MISSING_DEPENDENCIES,
                FailureLevel.INTERNAL_API_USAGES,
                FailureLevel.DEPRECATED_API_USAGES
            )
        )
    }
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        languageVersion.set(KotlinVersion.KOTLIN_2_2)
    }
}
