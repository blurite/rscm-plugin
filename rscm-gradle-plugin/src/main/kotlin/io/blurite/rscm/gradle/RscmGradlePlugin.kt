package io.blurite.rscm.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import java.io.File
import java.util.jar.JarFile

/** Configures RSCM validation for every supported JVM language present in a project. */
class RscmGradlePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val standaloneCompilerJar = standaloneCompilerJar
        val extension =
            target.extensions.create("rscm", RscmGradleExtension::class.java).apply {
                mappingsDirectory.convention(target.layout.projectDirectory.dir("rscm"))
            }

        configureJava(target, extension, standaloneCompilerJar)
        configureKotlin(target, standaloneCompilerJar != null)
    }

    private fun configureJava(
        target: Project,
        extension: RscmGradleExtension,
        standaloneCompilerJar: File?,
    ) {
        target.pluginManager.withPlugin("java") {
            val java = target.extensions.getByType(JavaPluginExtension::class.java)
            val annotationsDependency =
                standaloneCompilerJar?.let(target::files) ?: ANNOTATIONS_COORDINATE
            val javaCompilerDependency =
                standaloneCompilerJar?.let(target::files) ?: JAVA_COMPILER_COORDINATE
            java.sourceSets.configureEach { sourceSet ->
                target.dependencies.add(
                    sourceSet.compileOnlyConfigurationName,
                    annotationsDependency,
                )
                target.dependencies.add(
                    sourceSet.annotationProcessorConfigurationName,
                    javaCompilerDependency,
                )
            }

            target.tasks.withType(JavaCompile::class.java).configureEach { compileTask ->
                val arguments =
                    target.objects.newInstance(RscmJavaCompilerArgumentProvider::class.java).apply {
                        mappingsDirectory.set(extension.mappingsDirectory)
                    }
                compileTask.options.compilerArgumentProviders.add(arguments)
            }
        }
    }

    private fun configureKotlin(
        target: Project,
        standalone: Boolean,
    ) {
        for (pluginId in KOTLIN_PLUGIN_IDS) {
            target.pluginManager.withPlugin(pluginId) {
                val supportPlugin =
                    if (standalone) {
                        RscmStandaloneKotlinCompilerSupportPlugin::class.java
                    } else {
                        RscmKotlinCompilerSupportPlugin::class.java
                    }
                target.pluginManager.apply(supportPlugin)
            }
        }
    }

    companion object {
        const val COMPILER_PLUGIN_ID = "io.blurite.rscm"
        const val MAPPINGS_DIRECTORY_OPTION = "mappingsDirectory"
        const val ANNOTATIONS_COORDINATE = "io.blurite:rscm-annotations:1.0"
        const val JAVA_COMPILER_COORDINATE = "io.blurite:rscm-java-compiler:1.0"

        internal val standaloneCompilerJar: File? by lazy {
            val location =
                RscmGradlePlugin::class.java.protectionDomain.codeSource?.location
                    ?: return@lazy null
            val file = runCatching { File(location.toURI()) }.getOrNull() ?: return@lazy null
            if (!file.isFile) return@lazy null

            val isBundle =
                runCatching {
                    JarFile(file).use { jar ->
                        jar.getEntry("io/blurite/rscm/annotations/Rscm.class") != null &&
                            jar.getEntry(
                                "META-INF/services/org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar",
                            ) != null &&
                            jar.getEntry("META-INF/services/com.sun.source.util.Plugin") != null
                    }
                }.getOrDefault(false)
            file.takeIf { isBundle }
        }

        private val KOTLIN_PLUGIN_IDS =
            setOf(
                "org.jetbrains.kotlin.jvm",
                "org.jetbrains.kotlin.multiplatform",
                "org.jetbrains.kotlin.android",
            )
    }
}
