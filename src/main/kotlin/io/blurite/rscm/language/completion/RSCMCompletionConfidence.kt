package io.blurite.rscm.language.completion

import com.intellij.codeInsight.completion.CompletionConfidence
import com.intellij.psi.PsiElement
import com.intellij.util.ThreeState
import io.blurite.rscm.language.util.hasRscmSeparator
import io.blurite.rscm.language.util.isValidRscmPrefix
import io.blurite.rscm.language.util.rscmPrefix

/**
 * @author Chris
 * @since 12/5/2020
 */
abstract class RSCMCompletionConfidence : CompletionConfidence() {
    fun shouldSkipAutopopup(
        value: String,
        element: PsiElement,
    ): ThreeState {
        val project = element.project
        return if (value.hasRscmSeparator && value.rscmPrefix.isValidRscmPrefix(project)) ThreeState.NO else return ThreeState.UNSURE
    }
}
