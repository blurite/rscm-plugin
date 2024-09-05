package io.blurite.rscm.language

import com.intellij.lang.refactoring.RefactoringSupportProvider
import com.intellij.psi.PsiElement
import io.blurite.rscm.language.psi.RSCMProperty

/**
 * @author Chris
 * @since 12/4/2020
 */
class RSCMRefactoringSupportProvider : RefactoringSupportProvider() {
    override fun isMemberInplaceRenameAvailable(
        elementToRename: PsiElement,
        context: PsiElement?,
    ) = if (elementToRename is RSCMProperty) {
        true
    } else {
        super.isMemberInplaceRenameAvailable(elementToRename, context)
    }
}
