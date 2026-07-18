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
import kotlin.test.assertTrue

class RscmJavaGradlePluginTest {
    @TempDir
    lateinit var projectDirectory: Path

    @Test
    fun `compileJava reports unresolved RSCM literals in a pure Java project`() {
        createProject(
            source =
                """
                public class Example {
                    private final String reference = "item.missing";
                }
                """.trimIndent(),
        )

        val result = runner().buildAndFail()

        assertEquals(TaskOutcome.FAILED, result.task(":compileJava")?.outcome)
        assertContains(result.output, "Unresolved RSCM property: item.missing")
    }

    @Test
    fun `annotations are available automatically and constrain Java method arguments`() {
        createProject(
            source =
                """
                import io.blurite.rscm.annotations.Rscm;

                public class Example {
                    static String load(@Rscm("item") String reference) {
                        return reference;
                    }

                    private final String result = load("npc.hans");
                }
                """.trimIndent(),
        )

        val result = runner().buildAndFail()

        assertEquals(TaskOutcome.FAILED, result.task(":compileJava")?.outcome)
        assertContains(result.output, "Expected an RSCM reference of type 'item', but found: npc.hans")
    }

    @Test
    fun `Rscm constrains Java fields and later assignments`() {
        createProject(
            source =
                """
                import io.blurite.rscm.annotations.Rscm;

                public class Example {
                    @Rscm("item")
                    private String reference = "item.abyssal_whip";

                    void update() {
                        reference = "npc.hans";
                    }
                }
                """.trimIndent(),
        )

        val result = runner().buildAndFail()

        assertEquals(TaskOutcome.FAILED, result.task(":compileJava")?.outcome)
        assertContains(result.output, "Expected an RSCM reference of type 'item', but found: npc.hans")
    }

    @Test
    fun `Rscm constrains Java locals constructors and varargs`() {
        createProject(
            source =
                """
                import io.blurite.rscm.annotations.Rscm;

                public class Example {
                    Example(@Rscm("item") String reference) {}

                    static void load(@Rscm("item") String... references) {}

                    void validate() {
                        @Rscm("item") String local = "npc.hans";
                        new Example("npc.hans");
                        load("item.abyssal_whip", "npc.hans");
                    }
                }
                """.trimIndent(),
        )

        val result = runner().buildAndFail()
        val message = "Expected an RSCM reference of type 'item', but found: npc.hans"

        assertEquals(TaskOutcome.FAILED, result.task(":compileJava")?.outcome)
        assertTrue(
            result.output.windowed(message.length).count { it == message } >= 3,
            result.output,
        )
    }

    @Test
    fun `RscmIgnore suppresses Java field and parameter validation`() {
        createProject(
            source =
                """
                import io.blurite.rscm.annotations.RscmIgnore;

                public class Example {
                    @RscmIgnore
                    private String reference = "item.missing";

                    static String load(@RscmIgnore String reference) {
                        return reference;
                    }

                    void update() {
                        reference = "item.still_missing";
                        load("item.also_missing");
                    }
                }
                """.trimIndent(),
        )

        val result = runner().build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":compileJava")?.outcome)
    }

    @Test
    fun `Java string concatenations remain dynamic and are ignored`() {
        createProject(
            source =
                """
                public class Example {
                    private final int index = 1;
                    private final String reference = "item.missing_" + index;
                }
                """.trimIndent(),
        )

        val result = runner().build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":compileJava")?.outcome)
    }

    @Test
    fun `mapping-only changes invalidate Java compilation`() {
        createProject(
            source =
                """
                public class Example {
                    private final String reference = "item.abyssal_whip";
                }
                """.trimIndent(),
        )

        val firstBuild = runner().build()
        assertEquals(TaskOutcome.SUCCESS, firstBuild.task(":compileJava")?.outcome)

        projectDirectory.resolve("mappings/item.rscm").writeText("other_item=1\n")

        val secondBuild = runner().buildAndFail()
        assertEquals(TaskOutcome.FAILED, secondBuild.task(":compileJava")?.outcome)
        assertContains(secondBuild.output, "Unresolved RSCM property: item.abyssal_whip")
    }

    @Test
    fun `Java parameter constraints are preserved across project boundaries`() {
        createLibraryProject()

        val result = runner(":app:compileJava").buildAndFail()

        assertEquals(TaskOutcome.FAILED, result.task(":app:compileJava")?.outcome)
        assertContains(result.output, "Expected an RSCM reference of type 'item', but found: npc.hans")
    }

    private fun createProject(source: String) {
        writeSettings()
        projectDirectory.resolve("build.gradle.kts").writeText(
            """
            plugins {
                java
                id("io.blurite.rscm.compiler") version "1.0"
            }

            repositories {
                maven { url = uri("${repository()}") }
                mavenCentral()
            }

            rscm {
                mappingsDirectory.set(layout.projectDirectory.dir("mappings"))
            }
            """.trimIndent(),
        )
        projectDirectory.resolve("src/main/java").createDirectories()
        projectDirectory.resolve("src/main/java/Example.java").writeText(source)
        writeMappings()
    }

    private fun createLibraryProject() {
        writeSettings(includes = "include(\"app\", \"library\")")
        projectDirectory.resolve("build.gradle.kts").writeText(
            """
            import io.blurite.rscm.gradle.RscmGradleExtension

            plugins {
                id("io.blurite.rscm.compiler") version "1.0" apply false
            }

            subprojects {
                repositories {
                    maven { url = uri("${repository()}") }
                    mavenCentral()
                }

                pluginManager.withPlugin("java") {
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
            writeText("plugins { java }")
        }
        projectDirectory.resolve("app/build.gradle.kts").apply {
            parent.createDirectories()
            writeText(
                """
                plugins { java }

                dependencies {
                    implementation(project(":library"))
                }
                """.trimIndent(),
            )
        }
        projectDirectory.resolve("library/src/main/java/Library.java").apply {
            parent.createDirectories()
            writeText(
                """
                import io.blurite.rscm.annotations.Rscm;

                public final class Library {
                    public static String load(@Rscm("item") String reference) {
                        return reference;
                    }
                }
                """.trimIndent(),
            )
        }
        projectDirectory.resolve("app/src/main/java/App.java").apply {
            parent.createDirectories()
            writeText(
                """
                public final class App {
                    private final String result = Library.load("npc.hans");
                }
                """.trimIndent(),
            )
        }
        writeMappings()
    }

    private fun writeSettings(includes: String = "") {
        projectDirectory.resolve("settings.gradle.kts").writeText(
            """
            pluginManagement {
                repositories {
                    maven { url = uri("${repository()}") }
                    gradlePluginPortal()
                    mavenCentral()
                }
            }

            rootProject.name = "rscm-java-compiler-test"
            $includes
            """.trimIndent(),
        )
    }

    private fun writeMappings() {
        projectDirectory.resolve("mappings").createDirectories()
        projectDirectory.resolve("mappings/item.rscm").writeText("abyssal_whip=4151\n")
    }

    private fun repository(): String =
        checkNotNull(System.getProperty("rscm.test.repository")).replace('\\', '/')

    private fun runner(task: String = "compileJava"): GradleRunner =
        GradleRunner
            .create()
            .withProjectDir(projectDirectory.toFile())
            .withArguments(task, "--stacktrace")
            .forwardOutput()
}
