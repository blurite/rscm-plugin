package io.blurite.rscm.language

import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.tree.TokenSet
import io.blurite.rscm.language.annotator.RSCMAnnotator
import io.blurite.rscm.language.psi.RSCMProperty
import io.blurite.rscm.language.psi.RSCMTypes.COMMENT
import io.blurite.rscm.language.psi.RSCMTypes.KEY

/**
 * @author Chris
 * @since 12/4/2020
 */
class RSCMFindUsagesProvider : FindUsagesProvider {
    override fun getWordsScanner() =
        DefaultWordsScanner(
            RSCMLexerAdapter(),
            TokenSet.create(KEY),
            TokenSet.create(COMMENT),
            TokenSet.EMPTY,
        )

    override fun canFindUsagesFor(psiElement: PsiElement) = psiElement is PsiNamedElement

    override fun getHelpId(psiElement: PsiElement): String? = null

    override fun getType(element: PsiElement) =
        if (element is RSCMProperty) {
            "rscm property"
        } else {
            ""
        }

    override fun getDescriptiveName(element: PsiElement) =
        if (element is RSCMProperty) {
            element.key!!
        } else {
            ""
        }

    override fun getNodeText(
        element: PsiElement,
        useFullName: Boolean,
    ) = if (element is RSCMProperty) {
        element.key + RSCMAnnotator.RSCM_SEPARATOR_STR + element.value
    } else {
        ""
    }
}
