package io.blurite.rscm.compiler.fir

import io.blurite.rscm.core.RscmMappingIndex
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirCallChecker
import org.jetbrains.kotlin.fir.expressions.FirCall
import org.jetbrains.kotlin.fir.expressions.resolvedArgumentMapping

class RscmCallChecker(
    private val mappingIndex: RscmMappingIndex,
) : FirCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirCall) {
        val arguments = expression.resolvedArgumentMapping ?: return
        for ((argument, parameter) in arguments) {
            when (val directive = parameter.rscmDirective(context.session)) {
                null, RscmDirective.Ignore -> continue
                is RscmDirective.RequireType -> {
                    for (literalExpression in argument.directStringLiterals()) {
                        val literal = literalExpression.value as? String ?: continue
                        validateRscmLiteral(
                            expression = literalExpression,
                            literal = literal,
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
}
