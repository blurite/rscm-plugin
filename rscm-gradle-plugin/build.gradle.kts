import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.jvm.tasks.Jar

plugins {
    kotlin("jvm") version "2.4.0"
    `java-gradle-plugin`
    `maven-publish`
}

group = "io.blurite"
version = "2026.2"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin-api:2.4.0")

    testImplementation(kotlin("test-junit5"))
    testImplementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api:2.4.0")
    testImplementation(gradleTestKit())
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

tasks.processResources {
    val pluginVersion = project.version.toString()
    inputs.property("pluginVersion", pluginVersion)
    filesMatching("io/blurite/rscm/gradle/rscm-plugin.properties") {
        expand("pluginVersion" to pluginVersion)
    }
}

gradlePlugin {
    plugins {
        create("rscmCompiler") {
            id = "io.blurite.rscm.compiler"
            implementationClass = "io.blurite.rscm.gradle.RscmGradlePlugin"
            displayName = "RSCM compiler integration"
            description = "Adds compile-time validation for RSCM string references in Kotlin and Java sources"
        }
    }
}

publishing {
    repositories {
        maven {
            name = "test"
            url = rootProject.layout.buildDirectory.dir("test-maven-repository").get().asFile.toURI()
        }
    }
}

tasks.test {
    val compilerBundleJar = rootProject.tasks.named<Jar>("buildCompilerJar")

    dependsOn(compilerBundleJar)
    dependsOn(":rscm-annotations:publishMavenPublicationToTestRepository")
    dependsOn(":rscm-core:publishMavenPublicationToTestRepository")
    dependsOn(":rscm-kotlin-compiler:publishMavenPublicationToTestRepository")
    dependsOn(":rscm-java-compiler:publishMavenPublicationToTestRepository")
    dependsOn("publishAllPublicationsToTestRepository")
    useJUnitPlatform()
    systemProperty(
        "rscm.test.repository",
        rootProject.layout.buildDirectory.dir("test-maven-repository").get().asFile.absolutePath,
    )
    systemProperty("rscm.test.plugin.version", project.version.toString())
    systemProperty(
        "rscm.test.compiler.bundle",
        compilerBundleJar.flatMap { it.archiveFile }.get().asFile.absolutePath,
    )
}
