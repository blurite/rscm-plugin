package io.blurite.rscm.language.annotator

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import io.blurite.rscm.language.RSCMSyntaxHighlighter
import io.blurite.rscm.language.RSCMUtil
import io.blurite.rscm.language.RscmCreatePropertyQuickFix
import io.blurite.rscm.language.psi.RSCMFile

/**
 * @author Chris
 * @since 12/5/2020
 */
abstract class RSCMAnnotator : Annotator {
    // Define strings for the RSCM language prefix - used for annotations, line markers, etc.
    companion object {
        const val RSCM_SEPARATOR_STR = "."
    }

    abstract fun semiColon(): Boolean

    protected fun annotate(
        value: String,
        element: PsiElement,
        holder: AnnotationHolder,
    ) {
        if (!value.contains(RSCM_SEPARATOR_STR)) return
        val prefix = value.substringAfter("\"").substringBefore(RSCM_SEPARATOR_STR)
        val project = element.project
        if (!RSCMUtil.isValidPrefix(project, prefix)) return
        val path = RSCMUtil.constructPath(project, prefix)
        val vf = VirtualFileManager.getInstance().refreshAndFindFileByNioPath(path) ?: return
        val rscmFile = PsiManager.getInstance(element.project).findFile(vf) as? RSCMFile ?: return

        // Define the text ranges (start is inclusive, end is exclusive)
        // "prefix:key"
        //  01234567890
        val prefixRange = TextRange.from(element.textRange.startOffset + 1, prefix.length)
        val separatorRange = TextRange.from(prefixRange.endOffset, 0)
        val keyRange = TextRange(separatorRange.endOffset + 1, element.textRange.endOffset - 1)

        // highlight prefix and separator
        holder
            .newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(prefixRange)
            .textAttributes(DefaultLanguageHighlighterColors.KEYWORD)
            .create()
        // Highlight separator
        holder
            .newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(separatorRange)
            .textAttributes(RSCMSyntaxHighlighter.SEPARATOR)
            .create()

        // Highlight key
        val key = value.substringAfter(RSCM_SEPARATOR_STR).substringBefore("\"")
        val properties = RSCMUtil.findProperties(rscmFile, key)
        if (properties.isEmpty()) {
            if (key.isEmpty() || key.contains("$")) {
                return
            }
            holder
                .newAnnotation(HighlightSeverity.ERROR, "Unresolved property")
                .range(keyRange)
                .highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
                .withFix(RscmCreatePropertyQuickFix(prefix, key))
                .create()
        } else {
            // Found at least one property, force the text attributes to RSCM syntax value character
            holder
                .newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(keyRange)
                .textAttributes(RSCMSyntaxHighlighter.VALUE)
                .create()
        }
    }
}
