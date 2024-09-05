package io.blurite.rscm.language.psi

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import io.blurite.rscm.language.RSCMFileType

/**
 * @author Chris
 * @since 12/4/2020
 */
object RSCMElementFactory {
    fun createProperty(
        project: Project,
        name: String,
    ): RSCMProperty? {
        val file = createFile(project, name)
        return file.firstChild as RSCMProperty?
    }

    fun createFile(
        project: Project?,
        text: String,
    ): RSCMFile {
        val name = "dummy.rscm"
        return PsiFileFactory.getInstance(project).createFileFromText(name, RSCMFileType.INSTANCE, text) as RSCMFile
    }

    fun createProperty(
        project: Project,
        name: String,
        value: String,
    ): RSCMProperty? {
        val file = createFile(project, "$name:$value")
        return file.firstChild as RSCMProperty?
    }

    fun createCRLF(project: Project): PsiElement {
        val file = createFile(project, "\n")
        return file.firstChild
    }
}
