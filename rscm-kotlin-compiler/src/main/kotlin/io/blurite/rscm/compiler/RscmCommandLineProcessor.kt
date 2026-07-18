package io.blurite.rscm.compiler

import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration

@OptIn(ExperimentalCompilerApi::class)
class RscmCommandLineProcessor : CommandLineProcessor {
    override val pluginId: String = PLUGIN_ID

    override val pluginOptions: Collection<AbstractCliOption> = listOf(MAPPINGS_DIRECTORY_OPTION)

    override fun processOption(
        option: AbstractCliOption,
        value: String,
        configuration: CompilerConfiguration,
    ) {
        when (option.optionName) {
            MAPPINGS_DIRECTORY_OPTION.optionName -> configuration.put(RscmCompilerConfiguration.mappingsDirectory, value)
            else -> error("Unknown RSCM compiler option: ${option.optionName}")
        }
    }

    companion object {
        const val PLUGIN_ID = "io.blurite.rscm"
        const val MAPPINGS_DIRECTORY_OPTION_NAME = "mappingsDirectory"

        val MAPPINGS_DIRECTORY_OPTION =
            CliOption(
                optionName = MAPPINGS_DIRECTORY_OPTION_NAME,
                valueDescription = "<path>",
                description = "Directory containing .rscm mapping files",
                required = true,
                allowMultipleOccurrences = false,
            )
    }
}
