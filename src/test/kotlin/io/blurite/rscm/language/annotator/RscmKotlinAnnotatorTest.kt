package io.blurite.rscm.language.annotator

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.blurite.rscm.settings.RSCMProjectSettings
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText

class RscmKotlinAnnotatorTest : BasePlatformTestCase() {
    private lateinit var mappingsDirectory: Path

    override fun setUp() {
        super.setUp()

        mappingsDirectory = Files.createTempDirectory("rscm-annotator-test")
        mappingsDirectory.resolve("item.rscm").writeText("abyssal_whip=4151\n")
        RSCMProjectSettings.getInstance(project).mappingsPath = mappingsDirectory.toString()

        myFixture.addFileToProject(
            "io/blurite/rscm/annotations/RscmAnnotations.kt",
            """
            package io.blurite.rscm.annotations

            @Target(AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.LOCAL_VARIABLE)
            @Retention(AnnotationRetention.BINARY)
            annotation class Rscm(val type: String)

            @Target(AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.LOCAL_VARIABLE)
            @Retention(AnnotationRetention.BINARY)
            annotation class RscmIgnore
            """.trimIndent(),
        )
    }

    override fun tearDown() {
        try {
            if (::mappingsDirectory.isInitialized) {
                FileUtil.delete(mappingsDirectory.toFile())
            }
        } finally {
            super.tearDown()
        }
    }

    fun testRscmConstrainsPropertyLiterals() {
        val errors =
            errors(
                """
                import io.blurite.rscm.annotations.Rscm

                @Rscm("item")
                val reference = "npc.hans"
                """.trimIndent(),
            )

        assertTrue(
            "Errors: $errors",
            errors.any { it == "Expected an RSCM reference of type 'item', but found: npc.hans" },
        )
    }

    fun testRscmConstrainsMethodArguments() {
        val errors =
            errors(
                """
                import io.blurite.rscm.annotations.Rscm

                class Loader {
                    fun load(@Rscm("item") reference: String) = reference
                }

                val result = Loader().load("npc.hans")
                """.trimIndent(),
            )

        assertTrue(
            "Errors: $errors",
            errors.any { it == "Expected an RSCM reference of type 'item', but found: npc.hans" },
        )
    }

    fun testRscmDoesNotConstrainUnrelatedNestedPropertyLiterals() {
        val errors =
            errors(
                """
                import io.blurite.rscm.annotations.Rscm

                fun identity(value: String) = value

                @Rscm("item")
                val reference = identity("ordinary.dotted.value")
                """.trimIndent(),
            )

        assertFalse(
            "Errors: $errors",
            errors.any { it.contains("RSCM", ignoreCase = true) || it == "Unresolved property" },
        )
    }

    fun testRscmConstrainsLaterAssignments() {
        val errors =
            errors(
                """
                import io.blurite.rscm.annotations.Rscm

                @Rscm("item")
                var reference = "item.abyssal_whip"

                fun update() {
                    reference = "npc.hans"
                }
                """.trimIndent(),
            )

        assertTrue(
            "Errors: $errors",
            errors.any { it == "Expected an RSCM reference of type 'item', but found: npc.hans" },
        )
    }

    fun testRscmConstrainsParameterDefaults() {
        val errors =
            errors(
                """
                import io.blurite.rscm.annotations.Rscm

                fun load(@Rscm("item") reference: String = "npc.hans") = reference
                """.trimIndent(),
            )

        assertTrue(
            "Errors: $errors",
            errors.any { it == "Expected an RSCM reference of type 'item', but found: npc.hans" },
        )
    }

    fun testRscmIgnoreSuppressesPropertyAndParameterValidation() {
        val errors =
            errors(
                """
                import io.blurite.rscm.annotations.RscmIgnore

                @RscmIgnore
                var property = "item.missing"

                fun load(@RscmIgnore reference: String) = reference

                fun update() {
                    property = "item.still_missing"
                    load("item.also_missing")
                }
                """.trimIndent(),
            )

        assertFalse(
            "Errors: $errors",
            errors.any { it.contains("RSCM", ignoreCase = true) || it == "Unresolved property" },
        )
    }

    private fun errors(source: String): List<String> {
        myFixture.configureByText("Example.kt", source)
        DumbService.getInstance(project).waitForSmartMode()
        return myFixture
            .doHighlighting(HighlightSeverity.ERROR)
            .mapNotNull { it.description }
    }
}
