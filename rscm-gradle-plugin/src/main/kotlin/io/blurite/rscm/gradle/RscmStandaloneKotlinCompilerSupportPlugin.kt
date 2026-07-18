package io.blurite.rscm.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.PathSensitivity
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

/** Loads the Kotlin compiler plugin directly from the standalone bundle JAR. */
class RscmStandaloneKotlinCompilerSupportPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val compilerJar =
            checkNotNull(RscmGradlePlugin.standaloneCompilerJar) {
                "The standalone RSCM Kotlin loader must be run from the compiler bundle JAR"
            }
        val mappingsDirectory =
            target.extensions.getByType(RscmGradleExtension::class.java).mappingsDirectory
        val compilerArguments =
            mappingsDirectory.map { directory ->
                listOf(
                    "-Xplugin=${compilerJar.absolutePath}",
                    "-P",
                    "plugin:${RscmGradlePlugin.COMPILER_PLUGIN_ID}:" +
                        "${RscmGradlePlugin.MAPPINGS_DIRECTORY_OPTION}=${directory.asFile.absolutePath}",
                )
            }

        target.configurations.configureEach { configuration ->
            if (configuration.name == "compileOnly" || configuration.name.endsWith("CompileOnly")) {
                target.dependencies.add(configuration.name, target.files(compilerJar))
            }
        }

        target.tasks.configureEach { task ->
            val compileTask = task as? KotlinCompilationTask<*> ?: return@configureEach
            compileTask.inputs
                .file(compilerJar)
                .withPropertyName("rscmCompilerBundle")
                .withPathSensitivity(PathSensitivity.NONE)
            compileTask.inputs
                .dir(mappingsDirectory)
                .withPropertyName("rscmMappings")
                .withPathSensitivity(PathSensitivity.RELATIVE)
            compileTask.compilerOptions.freeCompilerArgs.addAll(compilerArguments)
        }
    }
}
