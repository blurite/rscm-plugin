package io.blurite.rscm.compiler.fir

import io.blurite.rscm.core.RscmMappingIndex
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirVariableAssignmentChecker
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.expressions.calleeReference
import org.jetbrains.kotlin.fir.references.toResolvedVariableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol

class RscmVariableAssignmentChecker(
    private val mappingIndex: RscmMappingIndex,
) : FirVariableAssignmentChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirVariableAssignment) {
        val property = expression.calleeReference?.toResolvedVariableSymbol() as? FirPropertySymbol ?: return
        when (val directive = property.rscmDirective(context.session)) {
            null, RscmDirective.Ignore -> return
            RscmDirective.Reject -> {
                for (knownString in expression.rValue.compileTimeStrings()) {
                    validateNotRscmLiteral(
                        expression = knownString.expression,
                        literal = knownString.value,
                        mappingIndex = mappingIndex,
                        context = context,
                        reporter = reporter,
                    )
                }
            }
            is RscmDirective.RequireType -> {
                for (knownString in expression.rValue.compileTimeStrings()) {
                    validateRscmLiteral(
                        expression = knownString.expression,
                        literal = knownString.value,
                        requiredType = directive.type,
                        mappingIndex = mappingIndex,
                        context = context,
                        reporter = reporter,
                    )
                }
            }
        }
    }
}
