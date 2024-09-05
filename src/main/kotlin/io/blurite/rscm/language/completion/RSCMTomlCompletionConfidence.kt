package io.blurite.rscm.language.completion

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.ThreeState
import org.toml.lang.psi.TomlKeySegment
import org.toml.lang.psi.TomlLiteral

/**
 * @author Chris | 5/3/21
 */
class RSCMTomlCompletionConfidence : RSCMCompletionConfidence() {
    override fun shouldSkipAutopopup(
        element: PsiElement,
        psiFile: PsiFile,
        offset: Int,
    ): ThreeState {
        if (element.parent !is TomlLiteral && element.parent !is TomlKeySegment) return ThreeState.UNSURE
        val value = element.parent.text
        return shouldSkipAutopopup(value, element.parent)
    }
}
