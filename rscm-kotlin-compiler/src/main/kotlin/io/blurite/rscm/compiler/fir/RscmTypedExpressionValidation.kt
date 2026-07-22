package io.blurite.rscm.compiler.fir

import io.blurite.rscm.core.RscmMappingIndex
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirVarargArgumentsExpression
import org.jetbrains.kotlin.fir.expressions.FirWrappedArgumentExpression
import org.jetbrains.kotlin.fir.expressions.calleeReference
import org.jetbrains.kotlin.fir.expressions.resolvedArgumentMapping
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.references.toResolvedVariableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.type

context(context: CheckerContext, reporter: DiagnosticReporter)
internal fun validateRscmTypedExpression(
    expression: FirExpression,
    expectedType: ConeKotlinType,
    mappingIndex: RscmMappingIndex,
    visited: MutableSet<FirPropertySymbol> = mutableSetOf(),
) {
    when (expression) {
        is FirWrappedArgumentExpression -> {
            validateRscmTypedExpression(expression.expression, expectedType, mappingIndex, visited)
            return
        }
        is FirVarargArgumentsExpression -> {
            for (argument in expression.arguments) {
                validateRscmTypedExpression(argument, expectedType, mappingIndex, visited)
            }
            return
        }
        else -> Unit
    }

    when (val directive = expectedType.rscmDirective(context.session)) {
        RscmDirective.Ignore -> return
        RscmDirective.Reject -> {
            for (knownString in expression.compileTimeStrings()) {
                validateNotRscmLiteral(
                    expression = knownString.expression,
                    literal = knownString.value,
                    mappingIndex = mappingIndex,
                    context = context,
                    reporter = reporter,
                )
            }
            return
        }
        is RscmDirective.RequireType -> {
            for (knownString in expression.compileTimeStrings()) {
                validateRscmLiteral(
                    expression = knownString.expression,
                    literal = knownString.value,
                    requiredType = directive.type,
                    mappingIndex = mappingIndex,
                    context = context,
                    reporter = reporter,
                )
            }
            return
        }
        null -> Unit
    }

    if (!expectedType.hasNestedRscmConstraint(context)) return

    if (expression is FirPropertyAccessExpression) {
        val property =
            expression.calleeReference.toResolvedVariableSymbol() as? FirPropertySymbol
                ?: return
        val initializer = property.compileTimeInitializer(visited) ?: return
        validateRscmTypedExpression(initializer, expectedType, mappingIndex, visited)
        return
    }

    val call = expression as? FirFunctionCall ?: return
    val callableId =
        call.calleeReference
            .toResolvedCallableSymbol()
            ?.callableId
            ?.asSingleFqName()
            ?.asString()
            ?: return
    if (callableId !in SINGLE_ELEMENT_FACTORY_CALLS) return

    val elementType = expectedType.typeArguments.singleOrNull()?.type ?: return
    val arguments = call.resolvedArgumentMapping?.keys ?: return
    for (argument in arguments) {
        validateRscmTypedExpression(argument, elementType, mappingIndex, visited)
    }
}

internal fun ConeKotlinType.hasNestedRscmConstraint(context: CheckerContext): Boolean =
    typeArguments.any { projection ->
        val argumentType = projection.type ?: return@any false
        argumentType.rscmDirective(context.session) != null ||
            argumentType.hasNestedRscmConstraint(context)
    }

private val SINGLE_ELEMENT_FACTORY_CALLS =
    setOf(
        "kotlin.arrayOf",
        "kotlin.collections.arrayListOf",
        "kotlin.collections.hashSetOf",
        "kotlin.collections.linkedSetOf",
        "kotlin.collections.listOf",
        "kotlin.collections.listOfNotNull",
        "kotlin.collections.mutableListOf",
        "kotlin.collections.mutableSetOf",
        "kotlin.collections.setOf",
        "kotlin.sequences.sequenceOf",
    )
