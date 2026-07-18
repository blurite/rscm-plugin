package io.blurite.rscm.language.annotator

import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.IndexNotReadyException
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.base.KaConstantValue
import org.jetbrains.kotlin.analysis.api.resolution.successfulFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.successfulVariableAccessCall
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

internal sealed interface RscmKotlinDirective {
    data object Ignore : RscmKotlinDirective

    data class RequireType(
        val type: String,
    ) : RscmKotlinDirective
}

internal object RscmKotlinAnnotationSupport {
    private val rscmClassId = ClassId.topLevel(FqName("io.blurite.rscm.annotations.Rscm"))
    private val ignoreClassId = ClassId.topLevel(FqName("io.blurite.rscm.annotations.RscmIgnore"))
    private val valueArgumentName = Name.identifier("value")
    private val typeArgumentName = Name.identifier("type")

    fun directiveFor(expression: KtStringTemplateExpression): RscmKotlinDirective? {
        if (DumbService.isDumb(expression.project)) return null
        if (expression.getStrictParentOfType<KtAnnotationEntry>() != null) return null

        return try {
            analyze(expression) {
                callArgumentDirective(expression)
                    ?: assignmentDirective(expression)
                    ?: parameterDefaultDirective(expression)
                    ?: propertyDirective(expression)
            }
        } catch (_: IndexNotReadyException) {
            null
        }
    }

    private fun KaSession.parameterDefaultDirective(
        expression: KtStringTemplateExpression,
    ): RscmKotlinDirective? {
        val parameter = expression.getStrictParentOfType<KtParameter>() ?: return null
        return when (val directive = parameter.symbol.annotations.rscmDirective()) {
            RscmKotlinDirective.Ignore -> directive
            is RscmKotlinDirective.RequireType ->
                directive.takeIf { KtPsiUtil.deparenthesize(parameter.defaultValue) === expression }
            null -> null
        }
    }

    private fun KaSession.assignmentDirective(
        expression: KtStringTemplateExpression,
    ): RscmKotlinDirective? {
        val assignment = expression.getStrictParentOfType<KtBinaryExpression>() ?: return null
        if (
            assignment.operationToken != KtTokens.EQ ||
            KtPsiUtil.deparenthesize(assignment.right) !== expression
        ) {
            return null
        }

        val left = assignment.left as? KtElement ?: return null
        val access = left.resolveToCall()?.successfulVariableAccessCall() ?: return null
        return access.signature.symbol.annotations.rscmDirective()
    }

    private fun KaSession.callArgumentDirective(
        expression: KtStringTemplateExpression,
    ): RscmKotlinDirective? {
        val valueArgument = expression.getStrictParentOfType<KtValueArgument>() ?: return null
        val argumentExpression = valueArgument.getArgumentExpression() ?: return null
        if (KtPsiUtil.deparenthesize(argumentExpression) !== expression) return null

        val callElement = valueArgument.getStrictParentOfType<KtCallElement>() ?: return null
        val call = callElement.resolveToCall()?.successfulFunctionCallOrNull() ?: return null
        val parameter = call.valueArgumentMapping[argumentExpression]?.symbol ?: return null
        return parameter.annotations.rscmDirective()
    }

    private fun KaSession.propertyDirective(
        expression: KtStringTemplateExpression,
    ): RscmKotlinDirective? {
        val property = expression.getStrictParentOfType<KtProperty>() ?: return null
        return when (val directive = property.symbol.annotations.rscmDirective()) {
            RscmKotlinDirective.Ignore -> directive
            is RscmKotlinDirective.RequireType ->
                directive.takeIf { KtPsiUtil.deparenthesize(property.initializer) === expression }
            null -> null
        }
    }

    private fun KaAnnotationList.rscmDirective(): RscmKotlinDirective? {
        if (contains(ignoreClassId)) return RscmKotlinDirective.Ignore

        val annotation = get(rscmClassId).firstOrNull() ?: return null
        val type =
            annotation.stringArgument(valueArgumentName)?.takeIf(String::isNotEmpty)
                ?: annotation.stringArgument(typeArgumentName)
                ?: return null
        return RscmKotlinDirective.RequireType(type)
    }

    private fun org.jetbrains.kotlin.analysis.api.annotations.KaAnnotation.stringArgument(name: Name): String? {
        val argument = arguments.firstOrNull { it.name == name } ?: return null
        val constant = argument.expression as? KaAnnotationValue.ConstantValue ?: return null
        val stringValue = constant.value as? KaConstantValue.StringValue ?: return null
        return stringValue.value
    }
}
