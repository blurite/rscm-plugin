package io.blurite.rscm.language

import com.intellij.openapi.project.Project
import com.intellij.psi.util.PsiTreeUtil
import io.blurite.rscm.language.psi.RSCMFile
import io.blurite.rscm.language.psi.RSCMProperty
import io.blurite.rscm.settings.RSCMProjectSettings
import java.nio.file.Path
import java.util.*

/**
 * @author Chris
 * @since 12/3/2020
 */
object RSCMUtil {
    /**
     * Searches the specified RSCM file for the RSCM property with the given key.
     */
    @JvmStatic
    fun findProperties(
        file: RSCMFile,
        key: String,
    ): List<RSCMProperty> {
        val result = mutableListOf<RSCMProperty>()
        val properties = PsiTreeUtil.getChildrenOfType(file, RSCMProperty::class.java)
        if (properties != null) {
            for (property in properties) {
                if (key == property.key) {
                    result.add(property)
                }
            }
        }
        return result
    }

    @JvmStatic
    fun findProperties(file: RSCMFile): List<RSCMProperty> {
        val result = mutableListOf<RSCMProperty>()
        val properties = PsiTreeUtil.getChildrenOfType(file, RSCMProperty::class.java)
        Collections.addAll(result, *properties)
        return result
    }

    fun isValidPrefix(
        project: Project,
        prefix: String,
    ): Boolean {
        val settings = RSCMProjectSettings.getInstance(project)
        val mappingDirectory = settings.mappingsPath
        return Path
            .of(mappingDirectory)
            .toFile()
            .listFiles()
            ?.map { it.nameWithoutExtension }
            ?.any { it == prefix } ?: false
    }

    fun constructPath(
        project: Project,
        prefix: String,
    ): Path {
        val settings = RSCMProjectSettings.getInstance(project)
        val mappingDirectory = settings.mappingsPath
        return Path.of(mappingDirectory, "$prefix.${RSCMFileType.INSTANCE.defaultExtension}")
    }
}
