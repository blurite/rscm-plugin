package io.blurite.rscm.compiler

import io.blurite.rscm.compiler.fir.RscmFirExtensionRegistrar
import io.blurite.rscm.core.RscmMappingIndex
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import java.nio.file.Path

@OptIn(ExperimentalCompilerApi::class)
class RscmCompilerPluginRegistrar : CompilerPluginRegistrar() {
    override val pluginId: String = RscmCommandLineProcessor.PLUGIN_ID

    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val messageCollector =
            configuration.get(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
        val mappingsDirectory = configuration.get(RscmCompilerConfiguration.mappingsDirectory)

        if (mappingsDirectory == null) {
            messageCollector.report(
                CompilerMessageSeverity.ERROR,
                "The RSCM compiler plugin requires the '${RscmCommandLineProcessor.MAPPINGS_DIRECTORY_OPTION_NAME}' option",
            )
            return
        }

        val mappingIndex =
            try {
                RscmMappingIndex.load(Path.of(mappingsDirectory))
            } catch (exception: IllegalArgumentException) {
                messageCollector.report(
                    CompilerMessageSeverity.ERROR,
                    exception.message ?: "Unable to load RSCM mappings",
                )
                return
            }

        FirExtensionRegistrarAdapter.registerExtension(RscmFirExtensionRegistrar(mappingIndex))
    }
}
