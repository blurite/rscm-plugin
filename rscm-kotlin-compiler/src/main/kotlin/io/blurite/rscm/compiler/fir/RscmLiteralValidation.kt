package io.blurite.rscm.compiler.fir

import io.blurite.rscm.compiler.diagnostics.RscmDiagnostics
import io.blurite.rscm.core.RscmMappingIndex
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression

internal fun validateRscmLiteral(
    expression: FirLiteralExpression,
    literal: String,
    requiredType: String,
    mappingIndex: RscmMappingIndex,
    context: CheckerContext,
    reporter: DiagnosticReporter,
) {
    if (requiredType !in mappingIndex.prefixes) {
        reporter.reportOn(
            expression.source,
            RscmDiagnostics.UNKNOWN_RSCM_TYPE,
            requiredType,
            context,
        )
        return
    }

    val separator = literal.indexOf('.')
    val actualType = literal.takeIf { separator > 0 }?.substring(0, separator)
    if (actualType != requiredType || separator == literal.lastIndex) {
        reporter.reportOn(
            expression.source,
            RscmDiagnostics.WRONG_RSCM_TYPE,
            requiredType,
            literal,
            context,
        )
        return
    }

    val unresolved = mappingIndex.unresolvedReference(literal)
    if (unresolved != null) {
        reporter.reportOn(
            expression.source,
            RscmDiagnostics.UNRESOLVED_PROPERTY,
            unresolved.literal,
            context,
        )
    }
}
