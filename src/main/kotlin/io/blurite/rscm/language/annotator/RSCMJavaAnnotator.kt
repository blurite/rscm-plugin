package io.blurite.rscm.language.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression
import io.blurite.rscm.language.RSCMUtil

/**
 * @author Chris
 * @since 12/3/2020
 */
class RSCMJavaAnnotator : RSCMAnnotator() {
    override fun annotate(
        element: PsiElement,
        holder: AnnotationHolder,
    ) {
        if (element !is PsiLiteralExpression) return
        val value = element.value as? String ?: return

        if (RscmJavaAnnotationSupport.isPartOfConcatenation(element)) return

        when (val directive = RscmJavaAnnotationSupport.directiveFor(element)) {
            RscmJavaDirective.Ignore -> return
            is RscmJavaDirective.RequireType -> {
                if (!RSCMUtil.isValidPrefix(element.project, directive.type)) {
                    reportError(element, holder, "Unknown RSCM type in @Rscm: ${directive.type}")
                    return
                }

                val separator = value.indexOf(RSCM_SEPARATOR_STR)
                val actualType = value.takeIf { separator > 0 }?.substring(0, separator)
                if (actualType != directive.type || separator == value.lastIndex) {
                    reportError(
                        element,
                        holder,
                        "Expected an RSCM reference of type '${directive.type}', but found: $value",
                    )
                    return
                }
            }
            null -> Unit
        }

        annotate(value, element, holder)
    }

    private fun reportError(
        element: PsiLiteralExpression,
        holder: AnnotationHolder,
        message: String,
    ) {
        holder
            .newAnnotation(HighlightSeverity.ERROR, message)
            .range(element)
            .highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
            .create()
    }

    override fun semiColon() = false
}
