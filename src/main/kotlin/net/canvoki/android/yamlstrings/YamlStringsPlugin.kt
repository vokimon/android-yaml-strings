package net.canvoki.android.yamlstrings

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.AndroidSourceSet
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register

/**
 * Plugin configuration
 */
abstract class YamlStringsExtension {
    abstract val defaultLanguage: Property<String>
    abstract val autoCompletedPartialLanguages: SetProperty<String>
}

/**
 * Plugin that generates Android strings.xml from YAML in each sourceSet
 */
class YamlStringsPlugin : Plugin<Project> {

    override fun apply(project: Project) {

        val dslName = "yamlStrings"
        val extension = project.extensions.create(
            dslName,
            YamlStringsExtension::class.java,
        )

        extension.defaultLanguage.convention(
            project.providers
                .gradleProperty(dslName + ".defaultLanguage")
                .orElse("en")
        )

        extension.autoCompletedPartialLanguages.convention(
            project.providers
                .gradleProperty(dslName + ".autoCompletedPartialLanguages")
                .map { value ->
                    value.split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                }
                .orElse(emptyList())
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
        extension: YamlStringsExtension
    ) {

        sourceSets.forEach { sourceSet ->

            val translationsDir = project.layout.projectDirectory
                .dir("src/${sourceSet.name}/translations")

            val sourceSetInfix =
                sourceSet.name.replaceFirstChar { it.uppercase() }

            val outputDir = project.layout.buildDirectory
                .dir("generated/translations/${sourceSet.name}/res")

            val taskProvider = project.tasks.register(
                "generate${sourceSetInfix}StringsFromYaml",
                YamlStringsTask::class.java
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
