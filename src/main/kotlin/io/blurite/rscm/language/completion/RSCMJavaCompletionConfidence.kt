package io.blurite.rscm.language.completion

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiLiteralExpression
import com.intellij.util.ThreeState
import io.blurite.rscm.language.util.minus

/**
 * @author Chris | 5/3/21
 */
class RSCMJavaCompletionConfidence : RSCMCompletionConfidence() {
    override fun shouldSkipAutopopup(
        element: PsiElement,
        psiFile: PsiFile,
        offset: Int,
    ): ThreeState {
        if (element.parent !is PsiLiteralExpression) return ThreeState.UNSURE
        val value = element.parent.text - '"'
        return shouldSkipAutopopup(value, element)
    }
}
