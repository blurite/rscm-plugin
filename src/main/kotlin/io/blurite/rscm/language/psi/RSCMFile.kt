package io.blurite.rscm.language.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.psi.FileViewProvider
import io.blurite.rscm.language.RSCMFileType
import io.blurite.rscm.language.RSCMLanguage

/**
 * @author Chris
 * @since 12/3/2020
 */
class RSCMFile(
    viewProvider: FileViewProvider,
) : PsiFileBase(viewProvider, RSCMLanguage) {
    override fun getFileType() = RSCMFileType.INSTANCE

    override fun toString() = RSCMFileType.INSTANCE.name
}
