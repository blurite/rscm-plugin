package io.blurite.rscm.language

import com.intellij.lang.refactoring.NamesValidator
import com.intellij.openapi.project.Project

/**
 * @author Chris
 * @since 12/5/2020
 */

/**
 * Allows for special keywords such as spaces and punctuation to be allowed in renaming process for RSCM properties.
 */
class RSCMNamesValidator : NamesValidator {
    override fun isKeyword(
        name: String,
        project: Project?,
    ) = false

    override fun isIdentifier(
        name: String,
        project: Project?,
    ) = true
}
