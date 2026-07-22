package io.blurite.rscm.language.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import io.blurite.rscm.language.RSCMUtil
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.getContentRange

/**
 * @author Chris
 * @since 12/5/2020
 */
class RSCMKotlinAnnotator : RSCMAnnotator() {
    override fun annotate(
        element: PsiElement,
        holder: AnnotationHolder,
    ) {
        if (element is KtNameReferenceExpression) {
            annotateKnownReference(element, holder)
            return
        }

        if (element !is KtStringTemplateExpression) return

        when (val directive = RscmKotlinExpressionAnnotationSupport.directiveFor(element)) {
            RscmKotlinDirective.Ignore -> return
            RscmKotlinDirective.Reject -> {
                if (element.hasInterpolation()) return

                val literal = element.getContentRange().substring(element.text)
                val separator = literal.indexOf(RSCM_SEPARATOR_STR)
                val prefix = literal.takeIf { separator > 0 }?.substring(0, separator)
                if (
                    prefix != null &&
                    separator < literal.lastIndex &&
                    RSCMUtil.isValidPrefix(element.project, prefix)
                ) {
                    reportError(
                        element,
                        holder,
                        "Expected a non-RSCM string, but found RSCM reference: $literal",
                    )
                }
                return
            }
            is RscmKotlinDirective.RequireType -> {
                if (element.hasInterpolation()) return

                val literal = element.getContentRange().substring(element.text)
                if (!RSCMUtil.isValidPrefix(element.project, directive.type)) {
                    reportError(
                        element,
                        holder,
                        "Unknown RSCM type in @Rscm: ${directive.type}",
                    )
                    return
                }

                val separator = literal.indexOf(RSCM_SEPARATOR_STR)
                val actualType = literal.takeIf { separator > 0 }?.substring(0, separator)
                if (actualType != directive.type || separator == literal.lastIndex) {
                    reportError(
                        element,
                        holder,
                        "Expected an RSCM reference of type '${directive.type}', but found: $literal",
                    )
                    return
                }
            }
            null -> Unit
        }

        val value = element.text ?: return
        annotate(value, element, holder)
    }

    private fun annotateKnownReference(
        expression: KtNameReferenceExpression,
        holder: AnnotationHolder,
    ) {
        val literal = expression.compileTimeLiteral() ?: return
        when (val directive = RscmKotlinExpressionAnnotationSupport.directiveFor(expression)) {
            null, RscmKotlinDirective.Ignore -> return
            RscmKotlinDirective.Reject -> {
                val separator = literal.indexOf(RSCM_SEPARATOR_STR)
                val prefix = literal.takeIf { separator > 0 }?.substring(0, separator)
                if (
                    prefix != null &&
                    separator < literal.lastIndex &&
                    RSCMUtil.isValidPrefix(expression.project, prefix)
                ) {
                    reportError(
                        expression,
                        holder,
                        "Expected a non-RSCM string, but found RSCM reference: $literal",
                    )
                }
            }
            is RscmKotlinDirective.RequireType -> {
                if (!RSCMUtil.isValidPrefix(expression.project, directive.type)) {
                    reportError(
                        expression,
                        holder,
                        "Unknown RSCM type in @Rscm: ${directive.type}",
                    )
                    return
                }

                val separator = literal.indexOf(RSCM_SEPARATOR_STR)
                val actualType = literal.takeIf { separator > 0 }?.substring(0, separator)
                if (actualType != directive.type || separator == literal.lastIndex) {
                    reportError(
                        expression,
                        holder,
                        "Expected an RSCM reference of type '${directive.type}', but found: $literal",
                    )
                }
            }
        }
    }

    private fun KtExpression.compileTimeLiteral(
        visited: MutableSet<KtProperty> = mutableSetOf(),
    ): String? {
        return when (this) {
            is KtStringTemplateExpression ->
                takeUnless(KtStringTemplateExpression::hasInterpolation)
                    ?.let { getContentRange().substring(text) }
            is KtNameReferenceExpression -> {
                val property =
                    references.asSequence()
                        .mapNotNull { it.resolve() as? KtProperty }
                        .firstOrNull()
                        ?: return null
                if (property.isVar || (!property.isLocal && !property.hasModifier(KtTokens.CONST_KEYWORD))) {
                    return null
                }
                if (!visited.add(property)) return null
                val initializer = KtPsiUtil.deparenthesize(property.initializer) as? KtExpression
                initializer?.compileTimeLiteral(visited)
            }
            else -> null
        }
    }

    private fun reportError(
        element: PsiElement,
        holder: AnnotationHolder,
        message: String,
    ) {
        val relativeRange = (element as? KtStringTemplateExpression)?.getContentRange()
        val absoluteRange =
            if (relativeRange == null || relativeRange.isEmpty) {
                element.textRange
            } else {
                TextRange(
                    element.textRange.startOffset + relativeRange.startOffset,
                    element.textRange.startOffset + relativeRange.endOffset,
                )
            }

        holder
            .newAnnotation(HighlightSeverity.ERROR, message)
            .range(absoluteRange)
            .highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
            .create()
    }

    override fun semiColon() = true
}
