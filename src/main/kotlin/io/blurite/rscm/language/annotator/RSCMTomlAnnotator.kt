package io.blurite.rscm.language.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import org.toml.lang.psi.TomlKeySegment
import org.toml.lang.psi.TomlLiteral

/**
 * @author Chris
 * @since 12/3/2020
 */
class RSCMTomlAnnotator : RSCMAnnotator() {
    override fun annotate(
        element: PsiElement,
        holder: AnnotationHolder,
    ) {
        if (element !is TomlLiteral && element !is TomlKeySegment) return
        val value = element.text ?: return
        annotate(value, element, holder)
    }

    override fun semiColon() = false
}
