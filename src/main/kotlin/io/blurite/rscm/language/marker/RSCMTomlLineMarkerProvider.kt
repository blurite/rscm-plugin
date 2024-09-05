package io.blurite.rscm.language.marker

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.toml.lang.psi.TomlKeySegment
import org.toml.lang.psi.TomlLiteral

/**
 * @author Chris
 * @since 12/5/2020
 */
class RSCMTomlLineMarkerProvider : RSCMLineMarkerProvider() {
    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>?>,
    ) {
        if (element !is LeafPsiElement && (element.parent !is TomlLiteral && element.parent !is TomlKeySegment)) return
        collectNavigationMarkers(element.text, element, result)
    }
}
