package io.blurite.rscm.gradle

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class RscmGradlePluginTest {
    @TempDir
    lateinit var projectDirectory: Path

    private val pluginVersion: String
        get() = checkNotNull(System.getProperty("rscm.test.plugin.version"))

    @Test
    fun `compileKotlin reports unresolved RSCM literals`() {
        createProject(
            source = "val reference = \"item.missing\"",
            mappings = "abyssal_whip=4151\n",
        )

        val result = runner().buildAndFail()

        assertEquals(TaskOutcome.FAILED, result.task(":compileKotlin")?.outcome)
        assertContains(result.output, "Unresolved RSCM property: item.missing")
    }

    @Test
    fun `compileKotlin ignores interpolated RSCM strings`() {
        createProject(
            source =
                """
                val index = 1
                val reference = "item.missing_${'$'}{index + 1}"
                """.trimIndent(),
            mappings = "abyssal_whip=4151\n",
        )

        val result = runner().build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":compileKotlin")?.outcome)
    }

    @Test
    fun `annotations are available automatically and constrain method arguments`() {
        createProject(
            source =
                """
                import io.blurite.rscm.annotations.Rscm

                fun load(@Rscm("item") reference: String) = reference

                val result = load("npc.hans")
                """.trimIndent(),
            mappings = "abyssal_whip=4151\n",
        )

        val result = runner().buildAndFail()

        assertEquals(TaskOutcome.FAILED, result.task(":compileKotlin")?.outcome)
        assertContains(result.output, "Expected an RSCM reference of type 'item', but found: npc.hans")
    }

    @Test
    fun `RscmIgnore suppresses validation through the Gradle integration`() {
        createProject(
            source =
                """
                import io.blurite.rscm.annotations.RscmIgnore

                fun load(@RscmIgnore reference: String) = reference

                val result = load("item.missing")
                """.trimIndent(),
            mappings = "abyssal_whip=4151\n",
        )

        val result = runner().build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":compileKotlin")?.outcome)
    }

    @Test
    fun `mapping-only changes invalidate an otherwise unchanged compilation`() {
        createProject(
            source = "val reference = \"item.abyssal_whip\"",
            mappings = "abyssal_whip=4151\n",
        )

        val firstBuild = runner().build()
        assertEquals(TaskOutcome.SUCCESS, firstBuild.task(":compileKotlin")?.outcome)

        projectDirectory.resolve("mappings/item.rscm").writeText("other_item=1\n")

        val secondBuild = runner().buildAndFail()
        assertEquals(TaskOutcome.FAILED, secondBuild.task(":compileKotlin")?.outcome)
        assertContains(secondBuild.output, "Unresolved RSCM property: item.abyssal_whip")
    }

    @Test
    fun `root subprojects configuration applies validation to Kotlin modules`() {
        createMultiProject(
            source = "val reference = \"item.missing\"",
            mappings = "abyssal_whip=4151\n",
        )

        val result = runner(":app:compileKotlin").buildAndFail()

        assertEquals(TaskOutcome.FAILED, result.task(":app:compileKotlin")?.outcome)
        assertContains(result.output, "Unresolved RSCM property: item.missing")
    }

    @Test
    fun `parameter constraints are preserved across project boundaries`() {
        createAnnotatedLibraryProject()

        val result = runner(":app:compileKotlin").buildAndFail()

        assertEquals(TaskOutcome.FAILED, result.task(":app:compileKotlin")?.outcome)
        assertContains(result.output, "Expected an RSCM reference of type 'item', but found: npc.hans")
    }

    private fun createProject(
        source: String,
        mappings: String,
    ) {
        val repository = checkNotNull(System.getProperty("rscm.test.repository")).replace('\\', '/')

        projectDirectory.resolve("settings.gradle.kts").writeText(
            """
            pluginManagement {
                repositories {
                    maven { url = uri("$repository") }
                    gradlePluginPortal()
                    mavenCentral()
                }
            }

            rootProject.name = "rscm-compiler-test"
            """.trimIndent(),
        )
        projectDirectory.resolve("build.gradle.kts").writeText(
            """
            plugins {
                kotlin("jvm") version "2.4.0"
                id("io.blurite.rscm.compiler") version "$pluginVersion"
            }

            repositories {
                maven { url = uri("$repository") }
                mavenCentral()
            }

            rscm {
                mappingsDirectory.set(layout.projectDirectory.dir("mappings"))
            }
            """.trimIndent(),
        )
        projectDirectory.resolve("src/main/kotlin").createDirectories()
        projectDirectory.resolve("src/main/kotlin/Example.kt").writeText(source)
        projectDirectory.resolve("mappings").createDirectories()
        projectDirectory.resolve("mappings/item.rscm").writeText(mappings)
    }

    private fun createMultiProject(
        source: String,
        mappings: String,
    ) {
        val repository = checkNotNull(System.getProperty("rscm.test.repository")).replace('\\', '/')

        projectDirectory.resolve("settings.gradle.kts").writeText(
            """
            pluginManagement {
                repositories {
                    maven { url = uri("$repository") }
                    gradlePluginPortal()
                    mavenCentral()
                }
            }

            rootProject.name = "rscm-multi-project-test"
            include("app")
            """.trimIndent(),
        )
        projectDirectory.resolve("build.gradle.kts").writeText(
            """
            import io.blurite.rscm.gradle.RscmGradleExtension

            plugins {
                kotlin("jvm") version "2.4.0" apply false
                id("io.blurite.rscm.compiler") version "$pluginVersion" apply false
            }

            subprojects {
                repositories {
                    maven { url = uri("$repository") }
                    mavenCentral()
                }

                pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
                    apply(plugin = "io.blurite.rscm.compiler")

                    extensions.configure<RscmGradleExtension> {
                        mappingsDirectory.set(rootProject.layout.projectDirectory.dir("mappings"))
                    }
                }
            }
            """.trimIndent(),
        )
        projectDirectory.resolve("app/build.gradle.kts").apply {
            parent.createDirectories()
            writeText(
                """
                plugins {
                    kotlin("jvm")
                }
                """.trimIndent(),
            )
        }
        projectDirectory.resolve("app/src/main/kotlin").createDirectories()
        projectDirectory.resolve("app/src/main/kotlin/Example.kt").writeText(source)
        projectDirectory.resolve("mappings").createDirectories()
        projectDirectory.resolve("mappings/item.rscm").writeText(mappings)
    }

    private fun createAnnotatedLibraryProject() {
        val repository = checkNotNull(System.getProperty("rscm.test.repository")).replace('\\', '/')

        projectDirectory.resolve("settings.gradle.kts").writeText(
            """
            pluginManagement {
                repositories {
                    maven { url = uri("$repository") }
                    gradlePluginPortal()
                    mavenCentral()
                }
            }

            rootProject.name = "rscm-library-test"
            include("app", "library")
            """.trimIndent(),
        )
        projectDirectory.resolve("build.gradle.kts").writeText(
            """
            import io.blurite.rscm.gradle.RscmGradleExtension

            plugins {
                kotlin("jvm") version "2.4.0" apply false
                id("io.blurite.rscm.compiler") version "$pluginVersion" apply false
            }

            subprojects {
                repositories {
                    maven { url = uri("$repository") }
                    mavenCentral()
                }

                pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
                    apply(plugin = "io.blurite.rscm.compiler")

                    extensions.configure<RscmGradleExtension> {
                        mappingsDirectory.set(rootProject.layout.projectDirectory.dir("mappings"))
                    }
                }
            }
            """.trimIndent(),
        )
        projectDirectory.resolve("library/build.gradle.kts").apply {
            parent.createDirectories()
            writeText(
                """
                plugins {
                    kotlin("jvm")
                }
                """.trimIndent(),
            )
        }
        projectDirectory.resolve("app/build.gradle.kts").apply {
            parent.createDirectories()
            writeText(
                """
                plugins {
                    kotlin("jvm")
                }

                dependencies {
                    implementation(project(":library"))
                }
                """.trimIndent(),
            )
        }
        projectDirectory.resolve("library/src/main/kotlin/Library.kt").apply {
            parent.createDirectories()
            writeText(
                """
                import io.blurite.rscm.annotations.Rscm

                fun load(@Rscm("item") reference: String) = reference
                """.trimIndent(),
            )
        }
        projectDirectory.resolve("app/src/main/kotlin/App.kt").apply {
            parent.createDirectories()
            writeText("val result = load(\"npc.hans\")")
        }
        projectDirectory.resolve("mappings").createDirectories()
        projectDirectory.resolve("mappings/item.rscm").writeText("abyssal_whip=4151\n")
    }

    private fun runner(task: String = "compileKotlin"): GradleRunner =
        GradleRunner
            .create()
            .withProjectDir(projectDirectory.toFile())
            .withArguments(
                task,
                "--stacktrace",
                "-Pkotlin.compiler.execution.strategy=in-process",
            )
            .forwardOutput()
}
