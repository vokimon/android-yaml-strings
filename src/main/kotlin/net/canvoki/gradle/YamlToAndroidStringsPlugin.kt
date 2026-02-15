package net.canvoki.gradle

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.AndroidSourceSet
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register

/**
 * Plugin configuration
 */
open class YamlToAndroidStringsExtension {
    var defaultLanguage: String = "en"
    var autoCompletedPartialLanguages: Set<String> = emptySet()
}

/**
 * Plugin that generates Android strings.xml from YAML in each sourceSet
 */
class YamlToAndroidStringsPlugin : Plugin<Project> {

    override fun apply(project: Project) {

        val extension = project.extensions.create(
            "yamlStrings",
            YamlToAndroidStringsExtension::class.java
        )

        project.plugins.withId("com.android.application") {
            val android =
                project.extensions.getByType<ApplicationExtension>()
            configure(project, android.sourceSets, extension)
        }

        project.plugins.withId("com.android.library") {
            val android =
                project.extensions.getByType<LibraryExtension>()
            configure(project, android.sourceSets, extension)
        }
    }

    private fun configure(
        project: Project,
        sourceSets: Iterable<AndroidSourceSet>,
        extension: YamlToAndroidStringsExtension
    ) {

        sourceSets.forEach { sourceSet ->

            val translationsDir = project.layout.projectDirectory
                .dir("src/${sourceSet.name}/translations")

            val sourceSetInfix =
                sourceSet.name.replaceFirstChar { it.uppercase() }

            val outputDir = project.layout.buildDirectory
                .dir("generated/translations/${sourceSet.name}/res")

            val taskProvider = project.tasks.register(
                "generate${sourceSetInfix}YamlToAndroidStrings",
                YamlToAndroidStringsTask::class.java
            ) {
                yamlInputFiles.from(
                    project.fileTree(translationsDir) {
                        include("**/*.yml", "**/*.yaml")
                    }
                )
                resDir.set(outputDir)
                defaultLanguage.set(extension.defaultLanguage)
                autoCompletedPartialLanguages.set(extension.autoCompletedPartialLanguages)
            }

            // Add the generated files as resources
            sourceSet.res.srcDir(outputDir)

            // Link to dependencies
            project.tasks.named("preBuild").configure {
                dependsOn(taskProvider)
            }
        }
    }
}
