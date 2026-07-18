package io.blurite.rscm.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText

class RscmMappingParserTest {
    @TempDir
    lateinit var directory: Path

    @Test
    fun `parses keys without treating colon as a separator`() {
        val file = directory.resolve("component.rscm")
        file.writeText(
            """
            # comment
            bank:universe=0
            plain_key=1
            escaped\\ key=value=containing=equals
            key_without_value
            ! another comment
            """.trimIndent(),
        )

        assertEquals(
            setOf("bank:universe", "plain_key", "escaped key", "key_without_value"),
            RscmMappingParser.parse(file),
        )
    }

    @Test
    fun `finds unresolved references only for known prefixes`() {
        directory.resolve("item.rscm").writeText("abyssal_whip=4151\n")
        val index = RscmMappingIndex.load(directory)

        assertNull(index.unresolvedReference("item.abyssal_whip"))
        assertNull(index.unresolvedReference("unknown.anything"))
        assertNull(index.unresolvedReference("not-a-reference"))
        assertEquals(
            RscmReference("item.missing", "item", "missing"),
            index.unresolvedReference("item.missing"),
        )
    }
}
