package io.blurite.rscm.compiler.diagnostics

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticRenderers.TO_STRING
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.error1
import org.jetbrains.kotlin.diagnostics.error2
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.psi.KtElement

object RscmDiagnostics : KtDiagnosticsContainer() {
    val UNRESOLVED_PROPERTY by error1<KtElement, String>()
    val UNKNOWN_RSCM_TYPE by error1<KtElement, String>()
    val WRONG_RSCM_TYPE by error2<KtElement, String, String>()
    val RSCM_NOT_ALLOWED by error1<KtElement, String>()

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = RscmDiagnosticRendererFactory
}

object RscmDiagnosticRendererFactory : BaseDiagnosticRendererFactory() {
    override val MAP: KtDiagnosticFactoryToRendererMap by
        KtDiagnosticFactoryToRendererMap("RSCM") { map ->
            map.put(
                RscmDiagnostics.UNRESOLVED_PROPERTY,
                "Unresolved RSCM property: {0}",
                TO_STRING,
            )
            map.put(
                RscmDiagnostics.UNKNOWN_RSCM_TYPE,
                "Unknown RSCM type in @Rscm: {0}",
                TO_STRING,
            )
            map.put(
                RscmDiagnostics.WRONG_RSCM_TYPE,
                "Expected an RSCM reference of type ''{0}'', but found: {1}",
                TO_STRING,
                TO_STRING,
            )
            map.put(
                RscmDiagnostics.RSCM_NOT_ALLOWED,
                "Expected a non-RSCM string, but found RSCM reference: {0}",
                TO_STRING,
            )
        }
}
