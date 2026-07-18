package io.blurite.rscm.compiler.fir

import io.blurite.rscm.core.RscmMappingIndex
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension

class RscmAdditionalCheckersExtension(
    session: FirSession,
    mappingIndex: RscmMappingIndex,
) : FirAdditionalCheckersExtension(session) {
    override val expressionCheckers: ExpressionCheckers =
        object : ExpressionCheckers() {
            override val callCheckers = setOf(RscmCallChecker(mappingIndex))
            override val literalExpressionCheckers = setOf(RscmStringLiteralChecker(mappingIndex))
            override val variableAssignmentCheckers = setOf(RscmVariableAssignmentChecker(mappingIndex))
        }
}
