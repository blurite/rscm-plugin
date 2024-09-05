package io.blurite.rscm.language

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.FoldingGroup
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import io.blurite.rscm.language.annotator.RSCMAnnotator
import io.blurite.rscm.language.psi.RSCMFile

/**
 * @author Chris
 * @since 12/4/2020
 */
class RSCMFoldingBuilder :
    FoldingBuilderEx(),
    DumbAware {
    override fun buildFoldRegions(
        root: PsiElement,
        document: Document,
        quick: Boolean,
    ): Array<FoldingDescriptor> {
        val descriptors: MutableList<FoldingDescriptor> = ArrayList()
        val virtualFiles = FileTypeIndex.getFiles(RSCMFileType.INSTANCE, GlobalSearchScope.allScope(root.project))
        for (virtualFile in virtualFiles) {
            val rscmFile = PsiManager.getInstance(root.project).findFile(virtualFile)
            if (rscmFile is RSCMFile) {
                val prefix = rscmFile.name.replace(".${RSCMFileType.INSTANCE.defaultExtension}", "")
                // Initialize the group of folding regions that will expand/collapse together.
                val group = FoldingGroup.newGroup(prefix)
                // Initialize the list of folding regions
                // Get a collection of the literal expressions in the document below root
                val literalExpressions = PsiTreeUtil.findChildrenOfType(root, PsiLiteralExpression::class.java)
                // Evaluate the collection
                for (literalExpression in literalExpressions) {
                    val value = if (literalExpression.value is String) literalExpression.value as String? else null
                    if (value != null && value.startsWith(prefix + RSCMAnnotator.RSCM_SEPARATOR_STR)) {
                        val key = value.substring(prefix.length + RSCMAnnotator.RSCM_SEPARATOR_STR.length)
                        // Get a list of all properties for a given key in the project
                        val properties = RSCMUtil.findProperties(rscmFile, key)
                        if (properties.size == 1) {
                            // Add a folding descriptor for the literal expression at this node.
                            descriptors.add(
                                FoldingDescriptor(
                                    literalExpression.node,
                                    TextRange(
                                        literalExpression.textRange.startOffset + 1,
                                        literalExpression.textRange.endOffset - 1,
                                    ),
                                    group,
                                ),
                            )
                        }
                    }
                }
            }
        }
        return descriptors.toTypedArray()
    }

    override fun getPlaceholderText(node: ASTNode): String {
        val retTxt = "..."
        if (node.psi !is PsiLiteralExpression) {
            return retTxt
        }
        val nodeElement = node.psi as PsiLiteralExpression
        if (nodeElement.value !is String) {
            return retTxt
        }
        val value = nodeElement.value as String
        val virtualFiles =
            FileTypeIndex.getFiles(RSCMFileType.INSTANCE, GlobalSearchScope.allScope(nodeElement.project))
        for (virtualFile in virtualFiles) {
            val rscmFile = PsiManager.getInstance(nodeElement.project).findFile(virtualFile)
            if (rscmFile is RSCMFile) {
                val prefix = rscmFile.name.replace(".${RSCMFileType.INSTANCE.defaultExtension}", "")
                if (!value.contains(prefix + RSCMAnnotator.RSCM_SEPARATOR_STR)) {
                    continue
                }
                val key = value.substring(prefix.length + RSCMAnnotator.RSCM_SEPARATOR_STR.length)
                val properties = RSCMUtil.findProperties(rscmFile, key)
                val place = properties.firstOrNull()?.value
                // IMPORTANT: keys can come with no values, so a test for null is needed
                // IMPORTANT: Convert embedded \n to backslash n, so that the string will look
                // like it has LF embedded in it and embedded " to escaped "
                return place?.replace("\n".toRegex(), "\\n")?.replace("\"".toRegex(), "\\\\\"") ?: retTxt
            }
        }
        return retTxt
    }

    override fun isCollapsedByDefault(node: ASTNode) = true
}
