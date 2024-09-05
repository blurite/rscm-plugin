package io.blurite.rscm.language.marker

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import io.blurite.rscm.language.RSCMIcons
import io.blurite.rscm.language.RSCMUtil
import io.blurite.rscm.language.annotator.RSCMAnnotator
import io.blurite.rscm.language.psi.RSCMFile

/**
 * @author Chris
 * @since 12/5/2020
 */
abstract class RSCMLineMarkerProvider : RelatedItemLineMarkerProvider() {
    fun collectNavigationMarkers(
        value: String,
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>?>,
    ) {
        if (!value.contains(RSCMAnnotator.RSCM_SEPARATOR_STR)) return
        val prefix = value.substringAfter("\"").substringBefore(RSCMAnnotator.RSCM_SEPARATOR_STR)
        val project = element.project
        if (!RSCMUtil.isValidPrefix(project, prefix)) return
        val path = RSCMUtil.constructPath(project, prefix)
        val vf = VirtualFileManager.getInstance().refreshAndFindFileByNioPath(path) ?: return
        val rscmFile = PsiManager.getInstance(element.project).findFile(vf) as? RSCMFile ?: return
        val possibleProperties = value.substring(prefix.length + RSCMAnnotator.RSCM_SEPARATOR_STR.length)
        val properties = RSCMUtil.findProperties(rscmFile, possibleProperties)
        if (properties.isEmpty()) return
        // Add the property to a collection of line marker info
        val builder =
            NavigationGutterIconBuilder
                .create(RSCMIcons.FILE)
                .setTargets(properties)
                .setTooltipText("Navigate to RSCM language property")
        result.add(builder.createLineMarkerInfo(element))
    }
}
