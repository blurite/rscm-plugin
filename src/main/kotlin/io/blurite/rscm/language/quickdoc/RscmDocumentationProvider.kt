package io.blurite.rscm.language.quickdoc

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import io.blurite.rscm.language.psi.RSCMProperty

class RscmDocumentationProvider : AbstractDocumentationProvider() {
    override fun generateHoverDoc(
        element: PsiElement,
        originalElement: PsiElement?,
    ): String? = generateDoc(element, originalElement)

    override fun getQuickNavigateInfo(
        element: PsiElement,
        originalElement: PsiElement?,
    ): String? {
        if (element is RSCMProperty) {
            val key: String = element.key ?: return null
            val value: String = element.value ?: return null
            return "\"$key\" = $value"
        }
        return null
    }

    override fun generateDoc(
        element: PsiElement,
        originalElement: PsiElement?,
    ): String? {
        if (element is RSCMProperty) {
            val key: String = element.key ?: return null
            val value: String = element.value ?: return null
            return renderFullDoc(key, value, element.containingFile.virtualFile)
        }
        return null
    }

    private fun renderFullDoc(
        key: String,
        value: String,
        file: VirtualFile,
    ): String {
        val sb = StringBuilder()
        sb.append(DocumentationMarkup.SECTIONS_START)
        addKeyValueSection("Key:", key, sb)
        addKeyValueSection("Value:", value, sb)
        addKeyValueSection("File:", "<a href=\"${file.url}\">${file.name}</a>", sb)
        sb.append(DocumentationMarkup.SECTIONS_END)
        return sb.toString()
    }

    private fun addKeyValueSection(
        key: String,
        value: String,
        sb: java.lang.StringBuilder,
    ) {
        sb.append(DocumentationMarkup.SECTION_HEADER_START)
        sb.append(key)
        sb.append(DocumentationMarkup.SECTION_SEPARATOR)
        sb.append("<p>")
        sb.append(value)
        sb.append(DocumentationMarkup.SECTION_END)
    }
}
