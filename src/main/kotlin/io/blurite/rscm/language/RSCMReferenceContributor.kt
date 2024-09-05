package io.blurite.rscm.language

import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import io.blurite.rscm.language.annotator.RSCMAnnotator
import io.blurite.rscm.language.psi.RSCMFile
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.toml.lang.psi.TomlKeySegment
import org.toml.lang.psi.TomlLiteral

/**
 * @author Chris
 * @since 12/4/2020
 */
@Suppress("NAME_SHADOWING")
class RSCMReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(PsiLiteralExpression::class.java),
            object : PsiReferenceProvider() {
                override fun getReferencesByElement(
                    element: PsiElement,
                    ctx: ProcessingContext,
                ): Array<PsiReference> {
                    val element = element as? PsiLiteralExpression ?: return PsiReference.EMPTY_ARRAY
                    val value = element.value as? String? ?: return PsiReference.EMPTY_ARRAY
                    return createReference(value, element)
                }
            },
        )
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(KtStringTemplateExpression::class.java),
            object : PsiReferenceProvider() {
                override fun getReferencesByElement(
                    element: PsiElement,
                    ctx: ProcessingContext,
                ): Array<PsiReference> {
                    val element = element as? KtStringTemplateExpression ?: return PsiReference.EMPTY_ARRAY
                    val value = element.text?.replace("\"", "") ?: return PsiReference.EMPTY_ARRAY
                    return createReference(value, element)
                }
            },
        )
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(TomlLiteral::class.java),
            object : PsiReferenceProvider() {
                override fun getReferencesByElement(
                    element: PsiElement,
                    ctx: ProcessingContext,
                ): Array<PsiReference> {
                    val element = element as? TomlLiteral ?: return PsiReference.EMPTY_ARRAY
                    val value = element.text?.replace("\"", "") ?: return PsiReference.EMPTY_ARRAY
                    return createReference(value, element)
                }
            },
        )
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(TomlKeySegment::class.java),
            object : PsiReferenceProvider() {
                override fun getReferencesByElement(
                    element: PsiElement,
                    ctx: ProcessingContext,
                ): Array<PsiReference> {
                    val element = element as? TomlKeySegment ?: return PsiReference.EMPTY_ARRAY
                    val value = element.text?.replace("\"", "") ?: return PsiReference.EMPTY_ARRAY
                    return createReference(value, element)
                }
            },
        )
    }

    fun createReference(
        value: String,
        element: PsiElement,
    ): Array<PsiReference> {
        val project = element.project
        val prefix = value.substringBefore(RSCMAnnotator.RSCM_SEPARATOR_STR)
        if (!RSCMUtil.isValidPrefix(project, prefix)) return PsiReference.EMPTY_ARRAY
        val path = RSCMUtil.constructPath(project, prefix)
        val vf = VirtualFileManager.getInstance().refreshAndFindFileByNioPath(path) ?: return PsiReference.EMPTY_ARRAY
        val rscmFile =
            PsiManager.getInstance(element.project).findFile(vf) as? RSCMFile ?: return PsiReference.EMPTY_ARRAY
        if (prefix.length >= value.length) return PsiReference.EMPTY_ARRAY
        val property = TextRange(prefix.length + RSCMAnnotator.RSCM_SEPARATOR_STR.length + 1, value.length + 1)
        return arrayOf(RSCMReference(rscmFile, element, property))
    }
}
