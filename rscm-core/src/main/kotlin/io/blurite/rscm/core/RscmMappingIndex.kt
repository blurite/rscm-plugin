package io.blurite.rscm.core

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension

class RscmMappingIndex private constructor(
    private val keysByPrefix: Map<String, Set<String>>,
) {
    val prefixes: Set<String>
        get() = keysByPrefix.keys

    fun unresolvedReference(literal: String): RscmReference? {
        val separator = literal.indexOf('.')
        if (separator <= 0 || separator == literal.lastIndex) return null

        val prefix = literal.substring(0, separator)
        val keys = keysByPrefix[prefix] ?: return null
        val key = literal.substring(separator + 1)
        if (key in keys) return null

        return RscmReference(literal, prefix, key)
    }

    fun contains(reference: RscmReference): Boolean =
        reference.key in keysByPrefix.getOrElse(reference.prefix) { emptySet() }

    companion object {
        fun load(directory: Path): RscmMappingIndex {
            if (!Files.isDirectory(directory)) {
                throw RscmMappingException("RSCM mappings directory does not exist or is not a directory: '$directory'")
            }

            val mappings = linkedMapOf<String, Set<String>>()
            try {
                Files.list(directory).use { paths ->
                    paths
                        .filter { Files.isRegularFile(it) && it.extension.equals("rscm", ignoreCase = true) }
                        .sorted()
                        .forEach { file -> mappings[file.nameWithoutExtension] = RscmMappingParser.parse(file) }
                }
            } catch (exception: RscmMappingException) {
                throw exception
            } catch (exception: Exception) {
                throw RscmMappingException("Unable to enumerate RSCM mappings directory '$directory'", exception)
            }

            return RscmMappingIndex(mappings)
        }
    }
}
