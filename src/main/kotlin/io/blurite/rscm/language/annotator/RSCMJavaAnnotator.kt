package io.blurite.rscm.language.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression

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
        annotate(value, element, holder)
    }

    override fun semiColon() = false
}
