package net.canvoki.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

class YamlToAndroidPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register("yamlToAndroidStrings", YamlToAndroidStringsTask::class.java)
    }
}
