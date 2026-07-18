package io.blurite.rscm.gradle

import org.gradle.api.file.DirectoryProperty

abstract class RscmGradleExtension {
    abstract val mappingsDirectory: DirectoryProperty
}
