package io.blurite.rscm.compiler.fir

import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.getAnnotationWithResolvedArgumentsByClassId
import org.jetbrains.kotlin.fir.declarations.getStringArgument
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.expressions.FirCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.FirVarargArgumentsExpression
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.expressions.FirWrappedArgumentExpression
import org.jetbrains.kotlin.fir.expressions.calleeReference
import org.jetbrains.kotlin.fir.expressions.resolvedArgumentMapping
import org.jetbrains.kotlin.fir.references.toResolvedVariableSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.ConstantValueKind

internal sealed interface RscmDirective {
    data object Ignore : RscmDirective

    data class RequireType(
        val type: String,
    ) : RscmDirective
}

internal object RscmAnnotationIds {
    val RSCM = ClassId.topLevel(FqName("io.blurite.rscm.annotations.Rscm"))
    val RSCM_IGNORE = ClassId.topLevel(FqName("io.blurite.rscm.annotations.RscmIgnore"))
    val VALUE_ARGUMENT = Name.identifier("value")
    val TYPE_ARGUMENT = Name.identifier("type")
}

internal fun FirAnnotationContainer.rscmDirective(session: FirSession): RscmDirective? {
    if (hasAnnotation(RscmAnnotationIds.RSCM_IGNORE, session)) return RscmDirective.Ignore

    val annotation = getAnnotationByClassId(RscmAnnotationIds.RSCM, session) ?: return null
    val type = annotation.rscmTypeArgument() ?: return null
    return RscmDirective.RequireType(type)
}

internal fun <D> FirBasedSymbol<D>.rscmDirective(session: FirSession): RscmDirective?
    where D : FirDeclaration, D : FirAnnotationContainer {
    if (hasAnnotation(RscmAnnotationIds.RSCM_IGNORE, session)) return RscmDirective.Ignore

    val annotation = getAnnotationWithResolvedArgumentsByClassId(RscmAnnotationIds.RSCM, session) ?: return null
    val type = annotation.rscmTypeArgument() ?: return null
    return RscmDirective.RequireType(type)
}

private fun org.jetbrains.kotlin.fir.expressions.FirAnnotation.rscmTypeArgument(): String? =
    getStringArgument(RscmAnnotationIds.VALUE_ARGUMENT)
        ?.takeIf(String::isNotEmpty)
        ?: getStringArgument(RscmAnnotationIds.TYPE_ARGUMENT)

internal fun CheckerContext.literalUsageDirective(expression: FirLiteralExpression): RscmDirective? {
    for (statement in callsOrAssignments.asReversed()) {
        when (statement) {
            is FirCall -> {
                val mapping = statement.resolvedArgumentMapping ?: continue
                for ((argument, parameter) in mapping) {
                    if (argument.directStringLiterals().none { it === expression }) continue
                    return parameter.rscmDirective(session)
                }
            }
            is FirVariableAssignment -> {
                if (statement.rValue.directStringLiterals().none { it === expression }) continue
                val property = statement.calleeReference?.toResolvedVariableSymbol() as? FirPropertySymbol ?: continue
                return property.rscmDirective(session)
            }
        }
    }
    return null
}

internal fun FirExpression.directStringLiterals(): List<FirLiteralExpression> =
    when (this) {
        is FirWrappedArgumentExpression -> expression.directStringLiterals()
        is FirVarargArgumentsExpression -> arguments.flatMap(FirExpression::directStringLiterals)
        is FirLiteralExpression ->
            if (kind == ConstantValueKind.String) {
                listOf(this)
            } else {
                emptyList()
            }
        else -> emptyList()
    }
