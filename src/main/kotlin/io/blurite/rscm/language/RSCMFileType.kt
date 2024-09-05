package io.blurite.rscm.language

import com.intellij.openapi.fileTypes.LanguageFileType

/**
 * @author Chris
 * @since 12/3/2020
 */
class RSCMFileType : LanguageFileType(RSCMLanguage) {
    override fun getName() = "RuneScape Config Mapping File"

    override fun getDescription() = "A file which provides a string to integer identity mapping."

    override fun getDefaultExtension() = "rscm"

    override fun getIcon() = RSCMIcons.FILE

    companion object {
        val INSTANCE = RSCMFileType()
    }
}
