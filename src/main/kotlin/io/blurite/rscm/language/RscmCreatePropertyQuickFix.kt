package io.blurite.rscm.language

import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import io.blurite.rscm.language.psi.RSCMElementFactory
import io.blurite.rscm.language.psi.RSCMFile

/**
 * @author Chris | 5/4/21
 */
class RscmCreatePropertyQuickFix(
    private val prefix: String,
    private val key: String,
) : BaseIntentionAction() {
    override fun getFamilyName() = "Create property"

    override fun getText() = "Create property $key"

    override fun isAvailable(
        project: Project,
        editor: Editor,
        file: PsiFile,
    ) = true

    override fun invoke(
        project: Project,
        editor: Editor,
        file: PsiFile,
    ) {
        ApplicationManager.getApplication().invokeLater {
            val virtualFiles = FileTypeIndex.getFiles(RSCMFileType.INSTANCE, GlobalSearchScope.allScope(project))
            val rscmFile = virtualFiles.find { it.name.startsWith(prefix) } ?: return@invokeLater
            createProperty(project, rscmFile)
        }
    }

    private fun createProperty(
        project: Project,
        file: VirtualFile,
    ) {
        WriteCommandAction.writeCommandAction(project).run<RuntimeException> {
            val simpleFile = PsiManager.getInstance(project).findFile(file) as RSCMFile? ?: return@run
            val lastChildNode = simpleFile.node.lastChildNode
            if (lastChildNode != null) {
                simpleFile.node.addChild(RSCMElementFactory.createCRLF(project).node)
            }
            val property = RSCMElementFactory.createProperty(project, key, "") ?: return@run
            simpleFile.node.addChild(property.node)
            (property.lastChild.navigationElement as Navigatable).navigate(true)
            FileEditorManager
                .getInstance(project)
                .selectedTextEditor!!
                .caretModel
                .moveCaretRelatively(2, 0, false, false, false)
        }
    }
}
