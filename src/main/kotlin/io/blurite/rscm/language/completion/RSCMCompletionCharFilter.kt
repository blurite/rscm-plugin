package io.blurite.rscm.language.completion

import com.intellij.codeInsight.lookup.CharFilter
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiLiteralExpression
import io.blurite.rscm.language.util.hasRscmSeparator
import io.blurite.rscm.language.util.isValidRscmPrefix
import io.blurite.rscm.language.util.minus
import io.blurite.rscm.language.util.rscmPrefix
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry

/**
 * @author Chris
 * @since 12/6/2020
 */
class RSCMCompletionCharFilter : CharFilter() {
    override fun acceptChar(
        c: Char,
        prefixLength: Int,
        lookup: Lookup?,
    ): Result? {
        lookup?.psiFile ?: return null
        val element = lookup.psiElement?.parent ?: return null
        val project = element.project
        return when (element) {
            is KtLiteralStringTemplateEntry -> acceptChar(element.text, project)
            is PsiLiteralExpression -> acceptChar(element.text - '"', project)
            else -> return null
        }
    }

    private fun acceptChar(
        value: String,
        project: Project,
    ) = if (value.hasRscmSeparator && value.rscmPrefix.isValidRscmPrefix(project)) {
        Result.ADD_TO_PREFIX
    } else {
        null
    }
}
