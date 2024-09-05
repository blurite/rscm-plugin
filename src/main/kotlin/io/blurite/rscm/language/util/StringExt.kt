package io.blurite.rscm.language.util

import com.intellij.openapi.project.Project
import io.blurite.rscm.language.RSCMUtil
import io.blurite.rscm.language.annotator.RSCMAnnotator

/**
 * @author Chris | 5/4/21
 */
val String.hasRscmSeparator: Boolean
    get() = contains(RSCMAnnotator.RSCM_SEPARATOR_STR)

val String.rscmPrefix: String
    get() = substringBefore(RSCMAnnotator.RSCM_SEPARATOR_STR)

fun String.isValidRscmPrefix(project: Project) = RSCMUtil.isValidPrefix(project, this)

fun String.remove(oldValue: String) = replace(oldValue, "")

operator fun String.minus(oldValue: String) = replace(oldValue, "")

operator fun String.minus(oldValue: Char) = replace(oldValue.toString(), "")
