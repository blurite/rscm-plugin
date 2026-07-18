package io.blurite.rscm.gradle

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class RscmCompilerBundleTest {
    @TempDir
    lateinit var projectDirectory: Path

    @Test
    fun `standalone compiler jar validates Kotlin consumers`() {
        copyCompilerBundle()
        writeSettings()
        projectDirectory.resolve("build.gradle.kts").writeText(
            """
            import io.blurite.rscm.gradle.RscmGradleExtension

            buildscript {
                dependencies {
                    classpath(files("gradle/rscm-compiler-1.0.jar"))
                }
            }

            plugins {
                kotlin("jvm") version "2.4.0"
            }

            apply(plugin = "io.blurite.rscm.compiler")

            repositories {
                mavenCentral()
            }

            extensions.configure<RscmGradleExtension> {
                mappingsDirectory.set(layout.projectDirectory.dir("mappings"))
            }
            """.trimIndent(),
        )
        projectDirectory.resolve("src/main/kotlin/Example.kt").apply {
            parent.createDirectories()
            writeText(
                """
                import io.blurite.rscm.annotations.Rscm

                fun load(@Rscm("item") reference: String) = reference
                val result = load("npc.hans")
                """.trimIndent(),
            )
        }
        writeMappings()

        val result = runner("compileKotlin", "-Pkotlin.compiler.execution.strategy=in-process").buildAndFail()

        assertEquals(TaskOutcome.FAILED, result.task(":compileKotlin")?.outcome)
        assertContains(result.output, "Expected an RSCM reference of type 'item', but found: npc.hans")
    }

    @Test
    fun `standalone compiler jar validates Java consumers`() {
        copyCompilerBundle()
        writeSettings()
        projectDirectory.resolve("build.gradle.kts").writeText(
            """
            import io.blurite.rscm.gradle.RscmGradleExtension

            buildscript {
                dependencies {
                    classpath(files("gradle/rscm-compiler-1.0.jar"))
                }
            }

            plugins {
                java
            }

            apply(plugin = "io.blurite.rscm.compiler")

            extensions.configure<RscmGradleExtension> {
                mappingsDirectory.set(layout.projectDirectory.dir("mappings"))
            }
            """.trimIndent(),
        )
        projectDirectory.resolve("src/main/java/Example.java").apply {
            parent.createDirectories()
            writeText(
                """
                import io.blurite.rscm.annotations.Rscm;

                public final class Example {
                    static String load(@Rscm("item") String reference) {
                        return reference;
                    }

                    private final String result = load("npc.hans");
                }
                """.trimIndent(),
            )
        }
        writeMappings()

        val result = runner("compileJava").buildAndFail()

        assertEquals(TaskOutcome.FAILED, result.task(":compileJava")?.outcome)
        assertContains(result.output, "Expected an RSCM reference of type 'item', but found: npc.hans")
    }

    private fun writeSettings() {
        projectDirectory.resolve("settings.gradle.kts").writeText(
            """
            pluginManagement {
                repositories {
                    gradlePluginPortal()
                    mavenCentral()
                }
            }

            rootProject.name = "rscm-compiler-bundle-test"
            """.trimIndent(),
        )
    }

    private fun writeMappings() {
        projectDirectory.resolve("mappings").createDirectories()
        projectDirectory.resolve("mappings/item.rscm").writeText("abyssal_whip=4151\n")
    }

    private fun copyCompilerBundle() {
        val source = Path.of(checkNotNull(System.getProperty("rscm.test.compiler.bundle")))
        val target = projectDirectory.resolve("gradle/rscm-compiler-1.0.jar")
        target.parent.createDirectories()
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING)
    }

    private fun runner(vararg arguments: String): GradleRunner =
        GradleRunner
            .create()
            .withProjectDir(projectDirectory.toFile())
            .withArguments(*arguments, "--stacktrace")
            .forwardOutput()
}
