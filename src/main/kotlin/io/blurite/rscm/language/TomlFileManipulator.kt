package io.blurite.rscm.language

import com.intellij.openapi.util.TextRange
import com.intellij.psi.AbstractElementManipulator
import org.toml.lang.psi.TomlFile

class TomlFileManipulator : AbstractElementManipulator<TomlFile>() {
    override fun handleContentChange(
        element: TomlFile,
        range: TextRange,
        newContent: String,
    ): TomlFile {
        element.name = "$newContent.toml"
        return element
    }
}
