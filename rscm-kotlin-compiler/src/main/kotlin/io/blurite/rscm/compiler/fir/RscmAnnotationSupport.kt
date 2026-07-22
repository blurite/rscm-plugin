package io.blurite.rscm.compiler.fir

import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.getAnnotationWithResolvedArgumentsByClassId
import org.jetbrains.kotlin.fir.declarations.getStringArgument
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.isConst
import org.jetbrains.kotlin.fir.expressions.FirCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirVarargArgumentsExpression
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.expressions.FirWrappedArgumentExpression
import org.jetbrains.kotlin.fir.expressions.calleeReference
import org.jetbrains.kotlin.fir.expressions.resolvedArgumentMapping
import org.jetbrains.kotlin.fir.references.toResolvedVariableSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.typeAnnotations
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.ConstantValueKind

internal sealed interface RscmDirective {
    data object Ignore : RscmDirective

    data object Reject : RscmDirective

    data class RequireType(
        val type: String,
    ) : RscmDirective
}

internal object RscmAnnotationIds {
    val RSCM = ClassId.topLevel(FqName("io.blurite.rscm.annotations.Rscm"))
    val RSCM_IGNORE = ClassId.topLevel(FqName("io.blurite.rscm.annotations.RscmIgnore"))
    val NOT_RSCM = ClassId.topLevel(FqName("io.blurite.rscm.annotations.NotRscm"))
    val VALUE_ARGUMENT = Name.identifier("value")
}

internal fun FirAnnotationContainer.rscmDirective(session: FirSession): RscmDirective? {
    val annotation = getAnnotationByClassId(RscmAnnotationIds.RSCM, session)
    if (hasAnnotation(RscmAnnotationIds.RSCM_IGNORE, session)) return RscmDirective.Ignore
    if (hasAnnotation(RscmAnnotationIds.NOT_RSCM, session)) return RscmDirective.Reject
    val type = annotation?.rscmTypeArgument() ?: return null
    return RscmDirective.RequireType(type)
}

internal fun FirCallableDeclaration.rscmDirective(session: FirSession): RscmDirective? {
    val typeDirective = returnTypeRef.rscmDirective(session)
    if (typeDirective != null) return typeDirective
    return (this as FirAnnotationContainer).rscmDirective(session)
}

@OptIn(SymbolInternals::class)
internal fun FirCallableSymbol<*>.rscmDirective(session: FirSession): RscmDirective? {
    if (hasAnnotation(RscmAnnotationIds.RSCM_IGNORE, session)) return RscmDirective.Ignore
    if (hasAnnotation(RscmAnnotationIds.NOT_RSCM, session)) return RscmDirective.Reject
    // Resolving declaration annotation arguments can replace lazy FIR type refs, so preserve the
    // type-use directive first.
    val typeDirective =
        resolvedReturnTypeRef.rscmDirective(session)
            ?: resolvedReturnType.rscmDirective(session)
    if (typeDirective != null) return typeDirective

    val declarationAnnotation =
        getAnnotationWithResolvedArgumentsByClassId(RscmAnnotationIds.RSCM, session)
    return declarationAnnotation?.rscmDirective()
}

internal fun ConeKotlinType.rscmDirective(session: FirSession): RscmDirective? {
    val annotation = typeAnnotations.getAnnotationByClassId(RscmAnnotationIds.RSCM, session)
    if (typeAnnotations.hasAnnotation(RscmAnnotationIds.RSCM_IGNORE, session)) {
        return RscmDirective.Ignore
    }
    if (typeAnnotations.hasAnnotation(RscmAnnotationIds.NOT_RSCM, session)) {
        return RscmDirective.Reject
    }

    return annotation?.rscmDirective()
}

private fun org.jetbrains.kotlin.fir.expressions.FirAnnotation.rscmDirective(): RscmDirective? =
    rscmTypeArgument()?.let(RscmDirective::RequireType)

private fun org.jetbrains.kotlin.fir.expressions.FirAnnotation.rscmTypeArgument(): String? =
    getStringArgument(RscmAnnotationIds.VALUE_ARGUMENT)
        ?.takeIf(String::isNotEmpty)

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

internal data class RscmCompileTimeString(
    val expression: FirExpression,
    val value: String,
)

/** Resolves only values that are immutable and unconditionally known at compile time. */
internal fun FirExpression.compileTimeStrings(
    visited: MutableSet<FirPropertySymbol> = mutableSetOf(),
): List<RscmCompileTimeString> =
    when (this) {
        is FirWrappedArgumentExpression -> expression.compileTimeStrings(visited)
        is FirVarargArgumentsExpression -> arguments.flatMap { it.compileTimeStrings(visited) }
        is FirLiteralExpression ->
            if (kind == ConstantValueKind.String) {
                listOf(RscmCompileTimeString(this, value as String))
            } else {
                emptyList()
            }
        is FirPropertyAccessExpression -> {
            val property =
                calleeReference.toResolvedVariableSymbol() as? FirPropertySymbol
                    ?: return emptyList()
            val initializer = property.compileTimeInitializer(visited) ?: return emptyList()
            initializer.compileTimeStrings(visited).map { it.copy(expression = this) }
        }
        else -> emptyList()
    }

@OptIn(SymbolInternals::class)
internal fun FirPropertySymbol.compileTimeInitializer(
    visited: MutableSet<FirPropertySymbol>,
): FirExpression? {
    if (!visited.add(this)) return null
    val property = fir
    if (!property.isVal || (!property.isLocal && !property.isConst)) return null
    return property.initializer
}
