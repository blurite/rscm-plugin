package io.blurite.rscm.gradle

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.process.CommandLineArgumentProvider

abstract class RscmJavaCompilerArgumentProvider : CommandLineArgumentProvider {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val mappingsDirectory: DirectoryProperty

    override fun asArguments(): Iterable<String> {
        val directoryUri = mappingsDirectory.get().asFile.toURI().toASCIIString()
        return listOf("-Xplugin:RSCM mappingsDirectory=$directoryUri")
    }
}
