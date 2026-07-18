package io.blurite.rscm.language.annotator

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.blurite.rscm.settings.RSCMProjectSettings
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText

class RscmJavaAnnotatorTest : BasePlatformTestCase() {
    private lateinit var mappingsDirectory: Path

    override fun setUp() {
        super.setUp()

        mappingsDirectory = Files.createTempDirectory("rscm-java-annotator-test")
        mappingsDirectory.resolve("item.rscm").writeText("abyssal_whip=4151\n")
        RSCMProjectSettings.getInstance(project).mappingsPath = mappingsDirectory.toString()

        myFixture.addFileToProject(
            "io/blurite/rscm/annotations/Rscm.java",
            """
            package io.blurite.rscm.annotations;

            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;

            @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE})
            @Retention(RetentionPolicy.CLASS)
            public @interface Rscm {
                String value() default "";
                String type() default "";
            }
            """.trimIndent(),
        )
        myFixture.addFileToProject(
            "io/blurite/rscm/annotations/RscmIgnore.java",
            """
            package io.blurite.rscm.annotations;

            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;

            @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE})
            @Retention(RetentionPolicy.CLASS)
            public @interface RscmIgnore {}
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

    fun testRscmConstrainsFieldLiterals() {
        val errors =
            errors(
                """
                import io.blurite.rscm.annotations.Rscm;

                class Example {
                    @Rscm("item")
                    String reference = "npc.hans";
                }
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
                import io.blurite.rscm.annotations.Rscm;

                class Example {
                    static String load(@Rscm("item") String reference) {
                        return reference;
                    }

                    String result = load("npc.hans");
                }
                """.trimIndent(),
            )

        assertTrue(
            "Errors: $errors",
            errors.any { it == "Expected an RSCM reference of type 'item', but found: npc.hans" },
        )
    }

    fun testRscmConstrainsLaterAssignments() {
        val errors =
            errors(
                """
                import io.blurite.rscm.annotations.Rscm;

                class Example {
                    @Rscm("item")
                    String reference = "item.abyssal_whip";

                    void update() {
                        reference = "npc.hans";
                    }
                }
                """.trimIndent(),
            )

        assertTrue(
            "Errors: $errors",
            errors.any { it == "Expected an RSCM reference of type 'item', but found: npc.hans" },
        )
    }

    fun testRscmIgnoreSuppressesFieldAndParameterValidation() {
        val errors =
            errors(
                """
                import io.blurite.rscm.annotations.RscmIgnore;

                class Example {
                    @RscmIgnore
                    String reference = "item.missing";

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

        assertFalse(
            "Errors: $errors",
            errors.any { it.contains("RSCM", ignoreCase = true) || it == "Unresolved property" },
        )
    }

    fun testJavaConcatenationsAreIgnored() {
        val errors =
            errors(
                """
                class Example {
                    int index = 1;
                    String reference = "item.missing_" + index;
                }
                """.trimIndent(),
            )

        assertFalse(
            "Errors: $errors",
            errors.any { it.contains("RSCM", ignoreCase = true) || it == "Unresolved property" },
        )
    }

    private fun errors(source: String): List<String> {
        myFixture.configureByText("Example.java", source)
        DumbService.getInstance(project).waitForSmartMode()
        return myFixture
            .doHighlighting(HighlightSeverity.ERROR)
            .mapNotNull { it.description }
    }
}
