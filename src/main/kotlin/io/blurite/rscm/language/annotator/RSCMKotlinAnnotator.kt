package io.blurite.rscm.language.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

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
        val value = element.text ?: return
        annotate(value, element, holder)
    }

    override fun semiColon() = true
}
