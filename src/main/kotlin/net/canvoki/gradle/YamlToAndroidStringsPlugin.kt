package net.canvoki.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.GradleException
import java.io.File
import java.util.Locale
import com.android.build.api.variant.AndroidComponentsExtension
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

import org.w3c.dom.Element

/**
 * Plugin configuration
 */
open class YamlToAndroidStringsExtension {
    var defaultLanguage: String = "en"
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
        val srcDir = File(project.projectDir, "src")
        srcDir.listFiles()?.filter { it.isDirectory }?.forEach { sourceSetDir ->

            val translationsDir = File(sourceSetDir, "translations")
            if (!translationsDir.exists()) return@forEach

            val outputDir = File(sourceSetDir, "/res")
            // TODO: To build dir, requires adding as sourceSet to be processed
            //val outputDir = project.layout.buildDirectory.dir("generated/yaml-strings/${sourceSetDir.name}/res")

            val sourceSetInfix = sourceSetDir.name.replaceFirstChar { it.uppercase() }
            val taskProvider = project.tasks.register(
                "generate${sourceSetInfix}YamlToAndroidStrings",
                YamlToAndroidStringsTask::class.java
            ) {
                yamlDir.set(translationsDir)
                resDir.set(outputDir)
                defaultLanguage.set(extension.defaultLanguage)
            }
            project.plugins.withId("com.android.application") {
                project.tasks.named("preBuild").configure { dependsOn(taskProvider) }
            }
            project.plugins.withId("com.android.library") {
                project.tasks.named("preBuild").configure { dependsOn(taskProvider) }
            }
        }
    }
}

