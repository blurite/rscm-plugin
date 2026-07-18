package io.blurite.rscm.compiler.fir

import io.blurite.rscm.compiler.diagnostics.RscmDiagnostics
import io.blurite.rscm.core.RscmMappingIndex
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

class RscmFirExtensionRegistrar(
    private val mappingIndex: RscmMappingIndex,
) : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        registerDiagnosticContainers(RscmDiagnostics)
        +{ session: FirSession -> RscmAdditionalCheckersExtension(session, mappingIndex) }
    }
}
