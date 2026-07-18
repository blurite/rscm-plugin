package io.blurite.rscm.compiler

import org.jetbrains.kotlin.config.CompilerConfigurationKey

internal object RscmCompilerConfiguration {
    val mappingsDirectory = CompilerConfigurationKey.create<String>("RSCM mappings directory")
}
