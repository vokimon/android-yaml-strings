package net.canvoki.gradle

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.Locale
import java.util.SortedSet
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

typealias ParamList = List<String>
data class Translatable (
    val defaultValue: String,
    val paramList: ParamList,
)
typealias ParamCatalog = Map<String, Translatable>

fun escapeAndroidString(input: String): String {
    var escaped =
        input
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\t", "\\t")
            .replace("\r", "\\r")
    if (escaped.startsWith("??")) {
        escaped = "\\?" + escaped.substring(1)
    } else if (escaped.contains("??")) {
        escaped = escaped.replace("??", "\\?\\?")
    }
    return escaped
}

fun validateResourceName(name: String): String {
    if (!name.matches(Regex("^[a-z][a-z0-9_]*$"))) {
        throw GradleException(
            "Invalid resource name '$name'. Android resource names must start with a lowercase letter " +
                "and contain only lowercase letters, digits, and underscores.",
        )
    }
    return name
}


class MismatchedParamException(
    val paramName: String,
) : Exception(
        "Parameter '$paramName' not found in provided params.",
    )

fun extractParams(template: String): ParamList {
    val tempTemplate = template.replace("{{", "<escaped_open>")
    val regex = "\\{([^}:]+)(?::([^}]+))?}".toRegex()

    return regex
        .findAll(tempTemplate)
        .map { it.groupValues[1].trim() }
        .distinct()
        .toList()
}

fun flattenYamlMap(
    map: Map<*, *>,
    prefix: String = "",
): Map<String, String> {
    val result = mutableMapOf<String, String>()
    map.toString()
    prefix.toString()
    map.forEach { (key, value) ->
        val fullKey = "$prefix$key"
        when (value) {
            is Map<*, *> -> result.putAll(flattenYamlMap(value, "${fullKey}__"))
            is String -> result[fullKey] = value
        }
    }
    return result
}

fun parametersToXml(
    template: String,
    params: ParamList,
): String {
    val tempTemplate = template.replace("{{", "<escaped_open>")
    val regex = "\\{([^}:]+)(?::([^}]+))?}".toRegex()

    return regex
        .replace(tempTemplate) { match ->
            val paramName = match.groupValues[1].trim()
            val format =
                match.groupValues
                    .getOrNull(2)
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() } ?: "s"
            val index = params.indexOf(paramName)
            if (index < 0) {
                throw MismatchedParamException(paramName)
            }
            "%${index + 1}\$$format"
        }.replace("<escaped_open>", "{")
        .replace("}}", "}")
}

fun parameterOrderFromYaml(yamlFile: File): ParamCatalog {
    val mapper = ObjectMapper(YAMLFactory())
    val yamlContent = mapper.readValue(yamlFile, Map::class.java) as Map<*, *>
    val result = mutableMapOf<String, Translatable>()

    fun processKey(
        content: Map<*, *>,
        prefix: String = "",
    ) {
        content.forEach { (key, value) ->
            val fullKey = prefix + (key as String)
            when (value) {
                is Map<*, *> -> {
                    processKey(value, prefix = "${fullKey}__")
                }
                is String -> {
                    result[fullKey] = Translatable(
                        paramList = extractParams(value),
                        defaultValue = value,
                    )
                }
            }
        }
    }
    processKey(yamlContent)
    return result
}

fun mapToStringXml(
    map: Map<String, String>,
    resources: org.w3c.dom.Element,
    paramCatalog: ParamCatalog,
    onError: (String)->Unit,
) {
    val doc = resources.ownerDocument
    map.forEach { (key, value) ->
        val stringElem = doc.createElement("string")
        val resourceName = key.lowercase(Locale.ROOT)
        validateResourceName(resourceName)
        stringElem.setAttribute("name", resourceName)
        val paramList = paramCatalog[key]?.paramList ?: emptyList()
        val valueWithPositionalParameters =
            try {
                parametersToXml(value, paramList)
            } catch (e: MismatchedParamException) {
                onError("""Key "$key" has a parameter "${e.paramName}" not present in the original string.""")
                value // keep the old string and continue
            }
        stringElem.textContent = escapeAndroidString(valueWithPositionalParameters)
        resources.appendChild(stringElem)
    }
}

/**
 * Completes a partial translation map with default values from ParamCatalog.
 *
 * @param currentMap The current language's translation map (may be incomplete)
 * @param paramCatalog Catalog containing all keys with their default values
 * @return A complete map with all keys from paramCatalog, using currentMap values where available,
 *         and falling back to defaultValue from paramCatalog for missing keys.
 */
fun autoCompleteTranslations(
    currentMap: Map<String, String>,
    paramCatalog: ParamCatalog,
): Map<String, String> {
    val result = mutableMapOf<String, String>()
    paramCatalog.forEach { (key, translatable) ->
        result[key] = currentMap[key] ?: translatable.defaultValue
    }
    currentMap.forEach { (key, value) ->
        if (key !in paramCatalog) {
            result[key] = value
        }
    }
    return result
}

abstract class YamlToAndroidStringsTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:Optional
    abstract val yamlInputFiles: org.gradle.api.file.ConfigurableFileCollection

    @get:OutputDirectory
    abstract val resDir: DirectoryProperty

    @get:Input
    abstract val defaultLanguage: Property<String>

    @get:Input
    abstract val autoCompletedPartialLanguages: SetProperty<String>

    private val errors = mutableListOf<String>()

    @TaskAction
    fun run() {
        if (yamlInputFiles.isEmpty()) return

        logger.lifecycle("Translations directory found: ${yamlInputFiles}")

        val languageCodes = yamlInputFiles
            .map { it.nameWithoutExtension.lowercase(Locale.ROOT) }
            .toSortedSet()

        val defaultLang = defaultLanguage.get()
        val autoCompletedLanguages = autoCompletedPartialLanguages.get()

        if (defaultLang !in languageCodes) {
            throw GradleException(
                "Default language '$defaultLang' not found. Available: ${languageCodes.joinToString(", ")}"
            )
        }

        val outputResDir = resDir.get().asFile

        writeArraysFile(outputResDir, languageCodes)

        val paramCatalog =
            parameterOrderFromYaml(
                yamlInputFiles.first { it.nameWithoutExtension.lowercase() == defaultLang }
            )

        languageCodes.forEach { langCode ->
            val file =
                yamlInputFiles.first {
                    it.nameWithoutExtension.lowercase(Locale.ROOT) == langCode
                }

            val qualifier =
                if (langCode == defaultLang || langCode == "default") "values"
                else "values-$langCode"

            val targetDir = File(outputResDir, qualifier)
            targetDir.mkdirs()

            val xmlFile = File(targetDir, "strings.xml")
            convertYamlToAndroidXml(file, xmlFile, paramCatalog, fallbackToDefault = langCode in autoCompletedLanguages )
        }
    }

    private fun writeArraysFile(
        resDir: File,
        languageCodes: Set<String>,
    ) {
        val arraysFile = File(resDir, "values/arrays_languages.xml")
        arraysFile.parentFile.mkdirs()
        arraysFile.writeText(
            buildString {
                appendLine("""<?xml version="1.0" encoding="utf-8"?>""")
                appendLine("<!-- AUTO-GENERATED FILE. DO NOT EDIT MANUALLY. -->")
                appendLine("<!-- This file is generated automatically from YAML translations. -->")
                appendLine("<!-- Any manual changes will be overwritten. -->")
                appendLine("<resources>")
                appendLine("    <string-array name=\"supported_language_codes\">")
                languageCodes.forEach {
                    appendLine("        <item>$it</item>")
                }
                appendLine("    </string-array>")
                appendLine("</resources>")
            },
        )
        println("Generated language arrays: ${languageCodes.joinToString(", ")}")
    }

    private fun convertYamlToAndroidXml(
        yamlFile: File,
        xmlFile: File,
        paramCatalog: ParamCatalog,
        fallbackToDefault: Boolean = false,
    ) {
        val mapper = ObjectMapper(YAMLFactory())
        val yamlContent = mapper.readValue(yamlFile, Map::class.java) as Map<*, *>

        val docBuilder = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }.newDocumentBuilder()
        val doc = docBuilder.newDocument()

        val comment = doc.createComment(" AUTO-GENERATED from ${yamlFile.name}. DO NOT EDIT THIS FILE DIRECTLY! ")
        doc.appendChild(comment)

        val resources = doc.createElement("resources")
        doc.appendChild(resources)

        val flatYaml = flattenYamlMap(yamlContent)

        val completedYaml = if (fallbackToDefault) autoCompleteTranslations(flatYaml, paramCatalog) else flatYaml

        mapToStringXml(completedYaml, resources, paramCatalog, onError = { error ->
            errors.add("\n$error\n    File: $yamlFile\n")
        })

        val transformer =
            TransformerFactory.newInstance().newTransformer().apply {
                setOutputProperty(OutputKeys.INDENT, "yes")
                setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4")
                setOutputProperty(OutputKeys.ENCODING, "utf-8")
                setOutputProperty(OutputKeys.STANDALONE, "yes")
            }

        transformer.transform(DOMSource(doc), StreamResult(xmlFile))
    }
}
