package io.blurite.rscm.language

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import io.blurite.rscm.language.psi.RSCMFile
import io.blurite.rscm.language.psi.RSCMProperty

/**
 * @author Chris
 * @since 12/4/2020
 */
class RSCMReference(
    val file: RSCMFile,
    element: PsiElement,
    textRange: TextRange,
) : PsiReferenceBase<PsiElement>(element, textRange),
    PsiPolyVariantReference {
    private val key = getKey()

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val properties = RSCMUtil.findProperties(file, key)
        val results = mutableListOf<ResolveResult>()
        for (property in properties) {
            results.add(PsiElementResolveResult(property))
        }
        return results.toTypedArray()
    }

    override fun resolve(): PsiElement? {
        val resolveResults = multiResolve(false)
        return if (resolveResults.size == 1) resolveResults.first().element else null
    }

    override fun getVariants(): Array<Any?> {
        val properties: List<RSCMProperty> = RSCMUtil.findProperties(file)
        val variants: MutableList<LookupElement> = ArrayList()
        for (property in properties) {
            if (property.key != null && property.key!!.isNotEmpty()) {
                variants.add(
                    LookupElementBuilder
                        .create(property)
                        .withIcon(RSCMIcons.FILE)
                        .withTailText(" ${property.value}")
                        .withTypeText(property.containingFile.name),
                )
            }
        }
        return variants.toTypedArray()
    }

    private fun getKey() =
        if (element is PsiFile) {
            (element as PsiFile).name.substring(rangeInElement.startOffset, rangeInElement.endOffset)
        } else {
            element.text.substring(rangeInElement.startOffset, rangeInElement.endOffset)
        }
}
