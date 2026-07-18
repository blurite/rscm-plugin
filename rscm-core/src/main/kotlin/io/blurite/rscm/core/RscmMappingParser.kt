package io.blurite.rscm.core

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

object RscmMappingParser {
    fun parse(file: Path): Set<String> {
        try {
            Files.newBufferedReader(file, StandardCharsets.UTF_8).use { reader ->
                return reader
                    .lineSequence()
                    .mapNotNull(::parseKey)
                    .toCollection(linkedSetOf())
            }
        } catch (exception: RscmMappingException) {
            throw exception
        } catch (exception: Exception) {
            throw RscmMappingException("Unable to read RSCM mapping file '$file'", exception)
        }
    }

    internal fun parseKey(line: String): String? {
        if (line.isBlank()) return null

        val content = line.trimStart()
        if (content.startsWith('#') || content.startsWith('!')) return null

        val separator = line.indexOf('=')
        val key = if (separator >= 0) line.substring(0, separator) else line
        if (key.isEmpty()) return null

        return key.replace("\\\\ ", " ")
    }
}
