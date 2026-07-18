package io.blurite.rscm.compiler.fir

import io.blurite.rscm.compiler.diagnostics.RscmDiagnostics
import io.blurite.rscm.core.RscmMappingIndex
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.context.findClosest
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirExpressionChecker
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.FirStringConcatenationCall
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.types.ConstantValueKind

class RscmStringLiteralChecker(
    private val mappingIndex: RscmMappingIndex,
) : FirExpressionChecker<FirLiteralExpression>(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirLiteralExpression) {
        if (expression.kind != ConstantValueKind.String) return
        val literal = expression.value as? String ?: return

        // FIR exposes the constant chunks of an interpolated string as literals too.
        if (context.containingElements.dropLast(1).any { it is FirStringConcatenationCall }) return

        // Calls own validation of directly supplied literals so parameter annotations can override
        // the ambient property annotation without producing duplicate diagnostics.
        if (context.literalUsageDirective(expression) != null) return

        val parameter = context.findClosest<FirValueParameterSymbol>()
        when (val directive = parameter?.rscmDirective(context.session)) {
            RscmDirective.Ignore -> return
            is RscmDirective.RequireType -> {
                if (parameter.hasDirectDefaultValue(expression)) {
                    validateRscmLiteral(
                        expression = expression,
                        literal = literal,
                        requiredType = directive.type,
                        mappingIndex = mappingIndex,
                        context = context,
                        reporter = reporter,
                    )
                    return
                }
            }
            null -> Unit
        }

        val property = context.findClosest<FirPropertySymbol>()
        when (val directive = property?.rscmDirective(context.session)) {
            RscmDirective.Ignore -> return
            is RscmDirective.RequireType -> {
                if (property.hasDirectInitializer(expression)) {
                    validateRscmLiteral(
                        expression = expression,
                        literal = literal,
                        requiredType = directive.type,
                        mappingIndex = mappingIndex,
                        context = context,
                        reporter = reporter,
                    )
                    return
                }
            }
            null -> Unit
        }

        val unresolved = mappingIndex.unresolvedReference(literal) ?: return

        reporter.reportOn(
            expression.source,
            RscmDiagnostics.UNRESOLVED_PROPERTY,
            unresolved.literal,
            context,
        )
    }

    @OptIn(SymbolInternals::class)
    private fun FirPropertySymbol.hasDirectInitializer(expression: FirLiteralExpression): Boolean =
        fir.initializer === expression

    @OptIn(SymbolInternals::class)
    private fun FirValueParameterSymbol.hasDirectDefaultValue(expression: FirLiteralExpression): Boolean =
        fir.defaultValue === expression
}
