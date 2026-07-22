package io.blurite.rscm.language.annotator

import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.IndexNotReadyException
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.base.KaConstantValue
import org.jetbrains.kotlin.analysis.api.resolution.successfulFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.symbols.KaVariableSymbol
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaTypeArgumentWithVariance
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

internal object RscmKotlinExpressionAnnotationSupport {
    private val rscmClassId = ClassId.topLevel(FqName("io.blurite.rscm.annotations.Rscm"))
    private val ignoreClassId = ClassId.topLevel(FqName("io.blurite.rscm.annotations.RscmIgnore"))
    private val notRscmClassId = ClassId.topLevel(FqName("io.blurite.rscm.annotations.NotRscm"))
    private val valueArgumentName = Name.identifier("value")

    fun directiveFor(expression: KtExpression): RscmKotlinDirective? {
        if (DumbService.isDumb(expression.project)) return null
        if (expression.getStrictParentOfType<KtAnnotationEntry>() != null) return null

        return try {
            analyze(expression) {
                val constrained = expression.constraintExpression()
                callArgumentDirective(constrained)
                    ?: parameterDefaultDirective(constrained)
                    ?: propertyDirective(constrained)
                    ?: RscmKotlinAnnotationSupport.directiveFor(expression as? org.jetbrains.kotlin.psi.KtStringTemplateExpression ?: return@analyze null)
            }
        } catch (_: IndexNotReadyException) {
            null
        }
    }

    private fun KaSession.callArgumentDirective(
        constrained: ConstraintExpression,
    ): RscmKotlinDirective? {
        val valueArgument = constrained.root.getStrictParentOfType<KtValueArgument>() ?: return null
        val argumentExpression = valueArgument.getArgumentExpression() ?: return null
        if (KtPsiUtil.deparenthesize(argumentExpression) !== constrained.root) return null

        val callElement = valueArgument.getStrictParentOfType<KtCallElement>() ?: return null
        val call = callElement.resolveToCall()?.successfulFunctionCallOrNull() ?: return null
        val parameter = call.valueArgumentMapping[argumentExpression]?.symbol ?: return null
        return parameter.rscmDirective(constrained.depth)
    }

    private fun KaSession.parameterDefaultDirective(
        constrained: ConstraintExpression,
    ): RscmKotlinDirective? {
        val parameter = constrained.root.getStrictParentOfType<KtParameter>() ?: return null
        if (KtPsiUtil.deparenthesize(parameter.defaultValue) !== constrained.root) return null
        return parameter.symbol.rscmDirective(constrained.depth)
    }

    private fun KaSession.propertyDirective(
        constrained: ConstraintExpression,
    ): RscmKotlinDirective? {
        val property = constrained.root.getStrictParentOfType<KtProperty>() ?: return null
        if (KtPsiUtil.deparenthesize(property.initializer) !== constrained.root) return null
        return property.symbol.rscmDirective(constrained.depth)
    }

    private fun KaVariableSymbol.rscmDirective(depth: Int): RscmKotlinDirective? {
        if (depth == 0) {
            annotations.rscmDirective()?.let { return it }
        }

        var constrainedType = returnType
        repeat(depth) {
            val classType = constrainedType as? KaClassType ?: return null
            val projection =
                classType.typeArguments.singleOrNull() as? KaTypeArgumentWithVariance
                    ?: return null
            constrainedType = projection.type
        }
        return constrainedType.annotations.rscmDirective()
    }

    private fun KaAnnotationList.rscmDirective(): RscmKotlinDirective? {
        if (contains(ignoreClassId)) return RscmKotlinDirective.Ignore
        if (contains(notRscmClassId)) return RscmKotlinDirective.Reject

        val annotation = get(rscmClassId).firstOrNull() ?: return null
        val type =
            annotation.stringArgument(valueArgumentName)?.takeIf(String::isNotEmpty)
                ?: return null
        return RscmKotlinDirective.RequireType(type)
    }

    private fun org.jetbrains.kotlin.analysis.api.annotations.KaAnnotation.stringArgument(name: Name): String? {
        val argument = arguments.firstOrNull { it.name == name } ?: return null
        val constant = argument.expression as? KaAnnotationValue.ConstantValue ?: return null
        val stringValue = constant.value as? KaConstantValue.StringValue ?: return null
        return stringValue.value
    }

    private fun KtExpression.constraintExpression(): ConstraintExpression {
        var root = this
        var depth = 0
        while (true) {
            val valueArgument = root.getStrictParentOfType<KtValueArgument>() ?: break
            if (KtPsiUtil.deparenthesize(valueArgument.getArgumentExpression()) !== root) break
            val call = valueArgument.getStrictParentOfType<KtCallExpression>() ?: break
            if (call.calleeExpression?.text !in SINGLE_ELEMENT_FACTORIES) break
            root = call
            depth++
        }
        return ConstraintExpression(root, depth)
    }

    private data class ConstraintExpression(
        val root: KtExpression,
        val depth: Int,
    )

    private val SINGLE_ELEMENT_FACTORIES =
        setOf(
            "arrayOf",
            "arrayListOf",
            "hashSetOf",
            "linkedSetOf",
            "listOf",
            "listOfNotNull",
            "mutableListOf",
            "mutableSetOf",
            "sequenceOf",
            "setOf",
        )
}
