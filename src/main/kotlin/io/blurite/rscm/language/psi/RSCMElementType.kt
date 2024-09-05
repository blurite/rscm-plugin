package io.blurite.rscm.language.psi

import com.intellij.psi.tree.IElementType
import io.blurite.rscm.language.RSCMLanguage

/**
 * @author Chris
 * @since 12/3/2020
 */
class RSCMElementType(
    debugName: String,
) : IElementType(debugName, RSCMLanguage)
