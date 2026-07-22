package io.blurite.rscm.compiler.fir

import io.blurite.rscm.core.RscmMappingIndex
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.types.coneType

class RscmPropertyTypeChecker(
    private val mappingIndex: RscmMappingIndex,
) : FirPropertyChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirProperty) {
        val initializer = declaration.initializer ?: return
        val expectedType = declaration.returnTypeRef.coneType
        if (!expectedType.hasNestedRscmConstraint(context)) return
        validateRscmTypedExpression(initializer, expectedType, mappingIndex)
    }
}
