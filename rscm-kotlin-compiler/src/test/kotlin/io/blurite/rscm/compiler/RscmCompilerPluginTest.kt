package io.blurite.rscm.compiler

import io.blurite.rscm.annotations.Rscm
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class RscmCompilerPluginTest {
    @TempDir
    lateinit var directory: Path

    @Test
    fun `rejects an unresolved RSCM literal`() {
        val result = compile("val reference = \"item.missing\"")

        assertEquals(ExitCode.COMPILATION_ERROR, result.exitCode)
        assertContains(result.output, "unresolved RSCM property: item.missing")
        assertContains(result.output, "Example.kt:1")
    }

    @Test
    fun `accepts resolved and unrelated literals`() {
        val result =
            compile(
                """
                val resolved = "item.abyssal_whip"
                val unrelated = "unknown.missing"
                """.trimIndent(),
            )

        assertEquals(ExitCode.OK, result.exitCode, result.output)
    }

    @Test
    fun `ignores literal segments inside interpolated strings`() {
        val result =
            compile(
                """
                val index = 1
                val dynamic = "varbit.agility_pyramid_tilt_${'$'}{index + 1}"
                """.trimIndent(),
            )

        assertEquals(ExitCode.OK, result.exitCode, result.output)
    }

    @Test
    fun `Rscm constrains property literals to its declared type`() {
        val result =
            compile(
                """
                import io.blurite.rscm.annotations.Rscm

                @Rscm("item")
                val reference = "npc.hans"
                """.trimIndent(),
            )

        assertEquals(ExitCode.COMPILATION_ERROR, result.exitCode)
        assertContains(result.output, "expected an RSCM reference of type 'item', but found: npc.hans")
    }

    @Test
    fun `Rscm validates property keys after their type matches`() {
        val result =
            compile(
                """
                import io.blurite.rscm.annotations.Rscm

                @Rscm("item")
                val reference = "item.missing"
                """.trimIndent(),
            )

        assertEquals(ExitCode.COMPILATION_ERROR, result.exitCode)
        assertContains(result.output, "unresolved RSCM property: item.missing")
    }

    @Test
    fun `Rscm does not constrain unrelated literals nested in a property initializer`() {
        val result =
            compile(
                """
                import io.blurite.rscm.annotations.Rscm

                fun identity(value: String) = value

                @Rscm("item")
                val reference = identity("ordinary.dotted.value")
                """.trimIndent(),
            )

        assertEquals(ExitCode.OK, result.exitCode, result.output)
    }

    @Test
    fun `RscmIgnore suppresses validation on properties`() {
        val result =
            compile(
                """
                import io.blurite.rscm.annotations.RscmIgnore

                @RscmIgnore
                val reference = "item.missing"
                """.trimIndent(),
            )

        assertEquals(ExitCode.OK, result.exitCode, result.output)
    }

    @Test
    fun `Rscm constrains later property assignments`() {
        val result =
            compile(
                """
                import io.blurite.rscm.annotations.Rscm

                @Rscm("item")
                var reference = "item.abyssal_whip"

                fun update() {
                    reference = "npc.hans"
                }
                """.trimIndent(),
            )

        assertEquals(ExitCode.COMPILATION_ERROR, result.exitCode)
        assertContains(result.output, "expected an RSCM reference of type 'item', but found: npc.hans")
    }

    @Test
    fun `RscmIgnore suppresses validation on later property assignments`() {
        val result =
            compile(
                """
                import io.blurite.rscm.annotations.RscmIgnore

                @RscmIgnore
                var reference = "item.missing"

                fun update() {
                    reference = "item.still_missing"
                }
                """.trimIndent(),
            )

        assertEquals(ExitCode.OK, result.exitCode, result.output)
    }

    @Test
    fun `Rscm constrains literal method arguments`() {
        val result =
            compile(
                """
                import io.blurite.rscm.annotations.Rscm

                class Loader {
                    fun load(other: Int = 0, @Rscm("item") reference: String) = reference
                }

                val result = Loader().load(reference = "npc.hans")
                """.trimIndent(),
            )

        assertEquals(ExitCode.COMPILATION_ERROR, result.exitCode)
        assertContains(result.output, "expected an RSCM reference of type 'item', but found: npc.hans")
    }

    @Test
    fun `Rscm validates matching literal method arguments`() {
        val result =
            compile(
                """
                import io.blurite.rscm.annotations.Rscm

                fun load(@Rscm("item") reference: String) = reference

                val result = load("item.missing")
                """.trimIndent(),
            )

        assertEquals(ExitCode.COMPILATION_ERROR, result.exitCode)
        assertContains(result.output, "unresolved RSCM property: item.missing")
    }

    @Test
    fun `Rscm constrains literal parameter defaults`() {
        val result =
            compile(
                """
                import io.blurite.rscm.annotations.Rscm

                fun load(@Rscm("item") reference: String = "npc.hans") = reference
                """.trimIndent(),
            )

        assertEquals(ExitCode.COMPILATION_ERROR, result.exitCode)
        assertContains(result.output, "expected an RSCM reference of type 'item', but found: npc.hans")
    }

    @Test
    fun `RscmIgnore suppresses validation on method arguments`() {
        val result =
            compile(
                """
                import io.blurite.rscm.annotations.RscmIgnore

                fun load(@RscmIgnore reference: String) = reference

                val result = load("item.missing")
                """.trimIndent(),
            )

        assertEquals(ExitCode.OK, result.exitCode, result.output)
    }

    @Test
    fun `annotated parameters still permit dynamic values`() {
        val result =
            compile(
                """
                import io.blurite.rscm.annotations.Rscm

                fun load(@Rscm("item") reference: String) = reference

                val index = 1
                val result = load("item.missing_${'$'}index")
                """.trimIndent(),
            )

        assertEquals(ExitCode.OK, result.exitCode, result.output)
    }

    private fun compile(sourceText: String): CompilationResult {
        val mappingsDirectory = directory.resolve("mappings").createDirectories()
        mappingsDirectory.resolve("item.rscm").writeText("abyssal_whip=4151\n")
        mappingsDirectory.resolve("varbit.rscm").writeText("run_energy=173\n")

        val source = directory.resolve("Example.kt")
        source.writeText(sourceText)
        val outputDirectory = directory.resolve("classes").createDirectories()
        val pluginJar = checkNotNull(System.getProperty("rscm.compiler.plugin.jar"))
        val standardLibrary = Path.of(Unit::class.java.protectionDomain.codeSource.location.toURI())
        val annotations = Path.of(Rscm::class.java.protectionDomain.codeSource.location.toURI())
        val classpath = listOf(standardLibrary, annotations).joinToString(File.pathSeparator)

        val output = ByteArrayOutputStream()
        val exitCode =
            PrintStream(output, true, Charsets.UTF_8).use { stream ->
                K2JVMCompiler().exec(
                    stream,
                    "-no-stdlib",
                    "-no-reflect",
                    "-classpath",
                    classpath,
                    "-d",
                    outputDirectory.toString(),
                    "-Xplugin=$pluginJar",
                    "-P",
                    "plugin:${RscmCommandLineProcessor.PLUGIN_ID}:${RscmCommandLineProcessor.MAPPINGS_DIRECTORY_OPTION_NAME}=$mappingsDirectory",
                    source.toString(),
                )
            }

        return CompilationResult(exitCode, output.toString(Charsets.UTF_8))
    }

    private data class CompilationResult(
        val exitCode: ExitCode,
        val output: String,
    )
}
