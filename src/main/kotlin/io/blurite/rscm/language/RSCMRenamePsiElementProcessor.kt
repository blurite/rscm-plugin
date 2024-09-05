package io.blurite.rscm.language

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReference
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import io.blurite.rscm.language.psi.RSCMFile
import io.blurite.rscm.language.psi.impl.RSCMPropertyImpl
import org.jetbrains.kotlin.idea.core.util.toPsiFile

class RSCMRenamePsiElementProcessor : RenamePsiElementProcessor() {
    override fun canProcessElement(element: PsiElement) = element is RSCMPropertyImpl

    override fun findReferences(
        element: PsiElement,
        searchScope: SearchScope,
        searchInCommentsAndStrings: Boolean,
    ): MutableCollection<PsiReference> {
        val basic = super.findReferences(element, searchScope, searchInCommentsAndStrings)
        val project = element.project
        val scope = GlobalSearchScope.allScope(project)
        val prefix = element.containingFile.virtualFile.nameWithoutExtension
        val directory = getDirectory(element) ?: return basic
        val oldKey = element.text.substringBefore(":")
        val files = FilenameIndex.getVirtualFilesByName("$oldKey.toml", scope)
        val fileReferences =
            files
                // E.g If prefix is item we only want to rename the files in `items` dir
                .filter { it.path.contains("/$directory/") }
                .mapNotNull { it.toPsiFile(project) }
                .mapNotNull { createReference(prefix, oldKey, it) }
        basic.addAll(fileReferences)
        return basic
    }

    private fun getDirectory(element: PsiElement): String? {
        val project = element.project
        val scope = GlobalSearchScope.allScope(project)
        val prefix = element.containingFile.virtualFile.nameWithoutExtension
        val typeVf = FilenameIndex.getVirtualFilesByName("directory.conf", scope).firstOrNull() ?: return null
        val lines =
            FileDocumentManager
                .getInstance()
                .getDocument(typeVf)!!
                .text
                .lines()
        return lines
            .first { it.contains(prefix) }
            .substringAfter("=")
            .trim()
    }

    private fun createReference(
        prefix: String,
        key: String,
        element: PsiFile,
    ): PsiReference? {
        val project = element.project
        if (!RSCMUtil.isValidPrefix(project, prefix)) return null
        val path = RSCMUtil.constructPath(project, prefix)
        val vf = VirtualFileManager.getInstance().refreshAndFindFileByNioPath(path) ?: return null
        val rscmFile =
            PsiManager.getInstance(element.project).findFile(vf) as? RSCMFile ?: return null
        val property = TextRange(0, 0)
        return RSCMReference(rscmFile, element, property)
    }
}
