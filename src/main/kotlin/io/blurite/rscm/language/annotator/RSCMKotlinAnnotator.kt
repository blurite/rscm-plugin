package io.blurite.rscm.language.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import io.blurite.rscm.language.RSCMUtil
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
        if (element !is KtStringTemplateExpression) return

        when (val directive = RscmKotlinAnnotationSupport.directiveFor(element)) {
            RscmKotlinDirective.Ignore -> return
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

    private fun reportError(
        element: KtStringTemplateExpression,
        holder: AnnotationHolder,
        message: String,
    ) {
        val relativeRange = element.getContentRange()
        val absoluteRange =
            if (relativeRange.isEmpty) {
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
