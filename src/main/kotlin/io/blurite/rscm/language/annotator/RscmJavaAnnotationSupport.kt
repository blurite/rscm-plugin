package io.blurite.rscm.language.annotator

import com.intellij.openapi.project.DumbService
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.JavaTokenType
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAssignmentExpression
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiExpressionList
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiNewExpression
import com.intellij.psi.PsiParenthesizedExpression
import com.intellij.psi.PsiPolyadicExpression
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.PsiVariable

internal sealed interface RscmJavaDirective {
    data object Ignore : RscmJavaDirective
    data object Reject : RscmJavaDirective


    data class RequireType(
        val type: String,
    ) : RscmJavaDirective
}

internal object RscmJavaAnnotationSupport {
    private const val RSCM_ANNOTATION = "io.blurite.rscm.annotations.Rscm"
    private const val RSCM_IGNORE_ANNOTATION = "io.blurite.rscm.annotations.RscmIgnore"
    private const val NOT_RSCM_ANNOTATION = "io.blurite.rscm.annotations.NotRscm"

    fun directiveFor(expression: PsiLiteralExpression): RscmJavaDirective? {
        if (DumbService.isDumb(expression.project) || expression.hasParentOfType<PsiAnnotation>()) {
            return null
        }

        val directExpression = expression.outermostParenthesizedExpression()
        return callArgumentDirective(directExpression)
            ?: assignmentDirective(directExpression)
            ?: variableInitializerDirective(directExpression)
            ?: ignoredContainingVariableDirective(expression)
    }

    fun isPartOfConcatenation(expression: PsiLiteralExpression): Boolean {
        val directExpression = expression.outermostParenthesizedExpression()
        val parent = directExpression.parent as? PsiPolyadicExpression ?: return false
        return parent.operationTokenType == JavaTokenType.PLUS
    }

    private fun callArgumentDirective(expression: PsiExpression): RscmJavaDirective? {
        val arguments = expression.parent as? PsiExpressionList ?: return null
        val argumentIndex = arguments.expressions.indexOfFirst { it === expression }
        if (argumentIndex < 0) return null

        val method =
            when (val call = arguments.parent) {
                is PsiMethodCallExpression -> call.resolveMethod()
                is PsiNewExpression -> call.resolveConstructor()
                else -> null
            } ?: return null

        val parameters = method.parameterList.parameters
        if (parameters.isEmpty()) return null
        val parameterIndex =
            when {
                argumentIndex < parameters.size -> argumentIndex
                method.isVarArgs -> parameters.lastIndex
                else -> return null
            }
        return parameters[parameterIndex].rscmDirective()
    }

    private fun assignmentDirective(expression: PsiExpression): RscmJavaDirective? {
        val assignment = expression.parent as? PsiAssignmentExpression ?: return null
        if (assignment.operationTokenType != JavaTokenType.EQ || assignment.rExpression !== expression) {
            return null
        }

        val reference = assignment.lExpression as? PsiReferenceExpression ?: return null
        val variable = reference.resolve() as? PsiVariable ?: return null
        return variable.rscmDirective()
    }

    private fun variableInitializerDirective(expression: PsiExpression): RscmJavaDirective? {
        val variable = expression.parent as? PsiVariable ?: return null
        if (variable.initializer !== expression) return null
        return variable.rscmDirective()
    }

    private fun ignoredContainingVariableDirective(expression: PsiLiteralExpression): RscmJavaDirective? {
        var current: PsiElement? = expression.parent
        while (current != null) {
            if (current is PsiVariable) {
                return current.rscmDirective().takeIf { it == RscmJavaDirective.Ignore }
            }
            current = current.parent
        }
        return null
    }

    private fun PsiModifierListOwner.rscmDirective(): RscmJavaDirective? {
        val typeAnnotations = (this as? PsiVariable)?.type?.annotations.orEmpty()
        fun findAnnotation(qualifiedName: String): PsiAnnotation? =
            modifierList?.findAnnotation(qualifiedName)
                ?: typeAnnotations.firstOrNull { it.qualifiedName == qualifiedName }

        if (findAnnotation(RSCM_IGNORE_ANNOTATION) != null) {
            return RscmJavaDirective.Ignore
        }
        if (findAnnotation(NOT_RSCM_ANNOTATION) != null) {
            return RscmJavaDirective.Reject
        }

        val annotation = findAnnotation(RSCM_ANNOTATION) ?: return null
        val value = annotation.stringAttribute("value")
        val type = annotation.stringAttribute("type")
        return RscmJavaDirective.RequireType(value?.takeIf(String::isNotEmpty) ?: type.orEmpty())
    }

    private fun PsiAnnotation.stringAttribute(name: String): String? {
        val expression =
            findDeclaredAttributeValue(name)
                ?: findAttributeValue(name)
                ?: return null
        return JavaPsiFacade
            .getInstance(project)
            .constantEvaluationHelper
            .computeConstantExpression(expression) as? String
    }

    private fun PsiExpression.outermostParenthesizedExpression(): PsiExpression {
        var current = this
        while (true) {
            val parent = current.parent as? PsiParenthesizedExpression ?: return current
            if (parent.expression !== current) return current
            current = parent
        }
    }

    private inline fun <reified T : PsiElement> PsiElement.hasParentOfType(): Boolean {
        var current = parent
        while (current != null) {
            if (current is T) return true
            current = current.parent
        }
        return false
    }
}
