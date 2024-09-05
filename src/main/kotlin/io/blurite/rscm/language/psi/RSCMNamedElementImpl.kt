package io.blurite.rscm.language.psi

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.search.GlobalSearchScope

/**
 * @author Chris
 * @since 12/4/2020
 */
abstract class RSCMNamedElementImpl(
    node: ASTNode,
) : ASTWrapperPsiElement(node),
    RSCMNamedElement {
    override fun getUseScope() = GlobalSearchScope.allScope(project)
}
