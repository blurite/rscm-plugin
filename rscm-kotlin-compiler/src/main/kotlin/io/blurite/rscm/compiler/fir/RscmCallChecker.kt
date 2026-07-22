package io.blurite.rscm.compiler.fir

import io.blurite.rscm.core.RscmMappingIndex
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirCallChecker
import org.jetbrains.kotlin.fir.expressions.FirCall
import org.jetbrains.kotlin.fir.expressions.resolvedArgumentMapping
import org.jetbrains.kotlin.fir.types.coneType

class RscmCallChecker(
    private val mappingIndex: RscmMappingIndex,
) : FirCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirCall) {
        val arguments = expression.resolvedArgumentMapping ?: return
        for ((argument, parameter) in arguments) {
            val directive = parameter.rscmDirective(context.session)
            when (directive) {
                null ->
                    validateRscmTypedExpression(
                        argument,
                        parameter.returnTypeRef.coneType,
                        mappingIndex,
                    )
                RscmDirective.Ignore -> continue
                RscmDirective.Reject -> {
                    for (knownString in argument.compileTimeStrings()) {
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
                    for (knownString in argument.compileTimeStrings()) {
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
}
