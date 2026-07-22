package io.blurite.rscm.compiler.fir

import io.blurite.rscm.compiler.diagnostics.RscmDiagnostics
import io.blurite.rscm.core.RscmMappingIndex
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.expressions.FirExpression

internal fun validateRscmLiteral(
    expression: FirExpression,
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

internal fun validateNotRscmLiteral(
    expression: FirExpression,
    literal: String,
    context: CheckerContext,
    reporter: DiagnosticReporter,
    mappingIndex: RscmMappingIndex,
) {
    val separator = literal.indexOf('.')
    if (separator <= 0 || separator == literal.lastIndex) return
    if (literal.substring(0, separator) !in mappingIndex.prefixes) return

    reporter.reportOn(
        expression.source,
        RscmDiagnostics.RSCM_NOT_ALLOWED,
        literal,
        context,
    )
}
