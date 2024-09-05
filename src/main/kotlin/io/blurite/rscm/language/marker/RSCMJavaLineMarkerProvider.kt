package io.blurite.rscm.language.marker

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.impl.source.tree.java.PsiJavaTokenImpl

/**
 * @author Chris
 * @since 12/4/2020
 */
class RSCMJavaLineMarkerProvider : RSCMLineMarkerProvider() {
    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>?>,
    ) {
        // This must be an element with a literal expression as a parent
        if (element !is PsiJavaTokenImpl || element.getParent() !is PsiLiteralExpression) return
        // The literal expression must start with the RSCM language literal expression
        val literalExpression = element.getParent() as PsiLiteralExpression
        val value = literalExpression.value as? String? ?: return
        collectNavigationMarkers(value, element, result)
    }
}
