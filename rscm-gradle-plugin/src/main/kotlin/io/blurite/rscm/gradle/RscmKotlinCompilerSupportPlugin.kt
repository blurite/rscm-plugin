package io.blurite.rscm.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.PathSensitivity
import org.jetbrains.kotlin.gradle.plugin.FilesSubpluginOption
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class RscmKotlinCompilerSupportPlugin : KotlinCompilerPluginSupportPlugin {
    private lateinit var project: Project

    override fun apply(target: Project) {
        project = target
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean =
        kotlinCompilation.platformType == KotlinPlatformType.jvm ||
            kotlinCompilation.platformType == KotlinPlatformType.androidJvm

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>,
    ): Provider<List<SubpluginOption>> {
        project.dependencies.add(
            kotlinCompilation.defaultSourceSet.compileOnlyConfigurationName,
            RscmGradlePlugin.ANNOTATIONS_COORDINATE,
        )

        val mappingsDirectory =
            project.extensions.getByType(RscmGradleExtension::class.java).mappingsDirectory

        kotlinCompilation.compileTaskProvider.configure { compileTask ->
            compileTask.inputs
                .dir(mappingsDirectory)
                .withPropertyName("rscmMappings")
                .withPathSensitivity(PathSensitivity.RELATIVE)
        }

        return mappingsDirectory.map { directory ->
            listOf<SubpluginOption>(
                FilesSubpluginOption(
                    key = RscmGradlePlugin.MAPPINGS_DIRECTORY_OPTION,
                    files = listOf(directory.asFile),
                ),
            )
        }
    }

    override fun getCompilerPluginId(): String = RscmGradlePlugin.COMPILER_PLUGIN_ID

    override fun getPluginArtifact(): SubpluginArtifact =
        SubpluginArtifact(
            groupId = "io.blurite",
            artifactId = "rscm-kotlin-compiler",
            version = "1.0",
        )
}
