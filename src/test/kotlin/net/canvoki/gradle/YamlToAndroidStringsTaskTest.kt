package net.canvoki.gradle

import kotlin.io.path.createTempFile
import kotlin.io.path.writeText
import kotlin.test.Test
import net.canvoki.test.assertEquals
import kotlin.test.assertFailsWith
import org.w3c.dom.Document
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class YamlToAndroidStringsTaskTest {
    @Test
    fun `escapeAndroidString should escape special characters`() {
        val input = "Line1\nLine2\tTabbed\\Backslash\"Quote'Single"
        val expected = "Line1\\nLine2\\tTabbed\\\\Backslash\\\"Quote\\'Single"
        val actual = escapeAndroidString(input)
        assertEquals(expected, actual)
    }

    @Test
    fun `escapeAndroidString should escape double question marks`() {
        val input = "Do you mean ??"
        val expected = "Do you mean \\?\\?"
        val actual = escapeAndroidString(input)
        assertEquals(expected, actual)
    }

    private fun assertExtractParams(
        input: String,
        expected: List<String>,
    ) {
        val result = extractParams(input)
        assertEquals(
            expected,
            result,
            "Input: '$input'\nExpected: $expected\nActual: $result\n",
        )
    }

    @Test
    fun `extractParams with no params returns empty list`() {
        assertExtractParams(
            "Hello world",
            emptyList(),
        )
    }

    @Test
    fun `extractParams with one param without format returns one name with s`() {
        assertExtractParams(
            "Hello {name}",
            listOf("name"),
        )
    }

    @Test
    fun `extractParams with one param with format returns name and format`() {
        assertExtractParams(
            "Hello {name:d}",
            listOf("name"),
        )
    }

    @Test
    fun `extractParams with spaces inside brackets`() {
        assertExtractParams("{ name }", listOf("name"))
        assertExtractParams("{ name:d }", listOf("name"))
    }

    @Test
    fun `extractParams with spaces around colon`() {
        assertExtractParams("{name : d}", listOf("name"))
    }

    @Test
    fun `extractParams with multiple params returns correct list`() {
        assertExtractParams(
            "Hello {param1} and {param2:d}",
            listOf("param1", "param2"),
        )
    }

    @Test
    fun `extractParams with repeated param collects it just once`() {
        assertExtractParams(
            "Hello {name}, your age is {age}, {name} again.",
            listOf("name", "age"),
        )
    }

    @Test
    fun `extractParams ignores escaped curly braces`() {
        assertExtractParams(
            "Hello {{user}}",
            emptyList(),
        )
    }

    @Test
    fun `extractParams triple curly braces`() {
        assertExtractParams(
            "Hello {{{user}}}",
            listOf("user"),
        )
    }

    // format params

    private fun assertParametersToXml(
        template: String,
        params: List<String>,
        expected: String,
    ) {
        val result = parametersToXml(template, params)
        assertEquals(expected, result, "Template: '$template'\nExpected: '$expected'\nActual: '$result'\n")
    }

    @Test
    fun `parametersToXml with no params returns original string`() {
        assertParametersToXml(
            template = "Just a plain string",
            params = emptyList(),
            expected = "Just a plain string",
        )
    }

    @Test
    fun `parametersToXml replaces one param with numbered format`() {
        assertParametersToXml(
            template = "Hello {name}",
            params = listOf("name"),
            expected = "Hello %1\$s",
        )
    }

    @Test
    fun `parametersToXml replaces other param with numbered format`() {
        assertParametersToXml(
            template = "Bye {user}",
            params = listOf("user"),
            expected = "Bye %1\$s",
        )
    }

    @Test
    fun `parametersToXml with format spect, use that`() {
        assertParametersToXml(
            template = "Hello {user:d}",
            params = listOf("user"),
            expected = "Hello %1\$d",
        )
    }

    @Test
    fun `parametersToXml with missing param throws MismatchedParamException`() {
        val template = "Hello {second}"
        val params = listOf("first") // Missing "second"

        val exception =
            assertFailsWith<MismatchedParamException> {
                parametersToXml(template, params)
            }

        assertEquals(exception.paramName, "second")
    }

    @Test
    fun `parametersToXml trims spaces before name`() {
        assertParametersToXml(
            template = "Hello { name}",
            params = listOf("name"),
            expected = "Hello %1\$s",
        )
    }

    @Test
    fun `parametersToXml trims spaces after name`() {
        assertParametersToXml(
            template = "Hello {name }",
            params = listOf("name"),
            expected = "Hello %1\$s",
        )
    }

    @Test
    fun `parametersToXml trims spaces around format spec`() {
        assertParametersToXml(
            template = "Hello {name : spec }",
            params = listOf("name"),
            expected = "Hello %1\$spec",
        )
    }

    @Test
    fun `parametersToXml replaces multiple params with numbered format`() {
        assertParametersToXml(
            template = "Hello {first}, you are {age:d} years old",
            params = listOf("first", "age"),
            expected = "Hello %1\$s, you are %2\$d years old",
        )
    }

    @Test
    fun `parametersToXml with escapped braces do not substitute`() {
        assertParametersToXml(
            template = "Hello {{name}}",
            params = listOf("name"),
            expected = "Hello {name}",
        )
    }

    @Test
    fun `parametersToXml with triple braces takes inner`() {
        assertParametersToXml(
            template = "Hello {{{name}}}",
            params = listOf("name"),
            expected = "Hello {%1\$s}",
        )
    }

    fun assertParameterOrderFromYaml(
        yamlContent: String,
        expected: Map<String, List<String>>,
    ) {
        val yamlFile = createTempFile(prefix = "tempYaml", suffix = ".yaml")
        yamlFile.writeText(yamlContent)

        val result = parameterOrderFromYaml(yamlFile.toFile())

        val resultProcessed = result.mapValues { (_, value) -> value.paramList }

        assertEquals(expected, resultProcessed)
    }

    @Test
    fun `parameterOrderFromYaml single parameter`() {
        assertParameterOrderFromYaml(
            yamlContent = """
                greeting: "Hello {name}"
                """,
            expected = mapOf("greeting" to listOf("name")),
        )
    }

    @Test
    fun `parameterOrderFromYaml multiple parameters`() {
        assertParameterOrderFromYaml(
            yamlContent = """
                greeting: "Hello {name}, welcome to {place}"
            """,
            expected = mapOf("greeting" to listOf("name", "place")),
        )
    }

    @Test
    fun `parameterOrderFromYaml strings with no parameters`() {
        assertParameterOrderFromYaml(
            yamlContent = """
                farewell: "Goodbye"
            """,
            expected = mapOf("farewell" to emptyList<String>()),
        )
    }

    @Test
    fun `parameterOrderFromYaml ignores escaped curly braces`() {
        assertParameterOrderFromYaml(
            yamlContent = """
                info: "This is a {{escaped}} text."
            """,
            expected = mapOf("info" to emptyList<String>()),
        )
    }

    @Test
    fun `parameterOrderFromYaml ignores format specifiers`() {
        val yamlContent = """
        info: "This is a {param:number} text."
        """
        val expected = mapOf("info" to listOf("param"))

        assertParameterOrderFromYaml(yamlContent, expected)
    }

    @Test
    fun `parameterOrderFromYaml multiple texts`() {
        assertParameterOrderFromYaml(
            yamlContent = """
                greeting: "Hello {name}, welcome to {place}"
                farewell: "Bye {name}"
            """,
            expected =
                mapOf(
                    "greeting" to listOf("name", "place"),
                    "farewell" to listOf("name"),
                ),
        )
    }

    @Test
    fun `parameterOrderFromYaml hierarchical keys`() {
        assertParameterOrderFromYaml(
            yamlContent = """
                parent:
                   greeting: "Hello {name}"
            """,
            expected =
                mapOf(
                    "parent__greeting" to listOf("name"),
                ),
        )
    }

    @Test
    fun `parameterOrderFromYaml deep hierarchical keys`() {
        assertParameterOrderFromYaml(
            yamlContent = """
                grandpa:
                    parent:
                        greeting: "Hello {name}"
            """,
            expected =
                mapOf(
                    "grandpa__parent__greeting" to listOf("name"),
                ),
        )
    }
}

class FlattenYamlMapTest {

    @Test
    fun `single level kept it untouched`() {
        val input = mapOf("key" to "value")
        val expected = mapOf("key" to "value")
        assertEquals(expected, flattenYamlMap(input))
    }

    @Test
    fun `nested flattens with double underscore`() {
        val input = mapOf(
            "parent" to mapOf(
                "child" to "nested_value"
            )
        )
        val expected = mapOf("parent__child" to "nested_value")
        assertEquals(expected, flattenYamlMap(input))
    }

    @Test
    fun `multiple levels of nesting`() {
        val input = mapOf(
            "a" to mapOf(
                "b" to mapOf(
                    "c" to "deep_value"
                )
            )
        )
        val expected = mapOf("a__b__c" to "deep_value")
        assertEquals(expected, flattenYamlMap(input))
    }

    @Test
    fun `handles empty map`() {
        val input = emptyMap<Any, Any>()
        val expected = emptyMap<String, String>()
        assertEquals(expected, flattenYamlMap(input))
    }
}

class MapToStringXmlTest {

    private fun createResourcesElement(): Pair<Document, org.w3c.dom.Element> {
        val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc = docBuilder.newDocument()
        val resources = doc.createElement("resources")
        doc.appendChild(resources)
        return doc to resources
    }

    private fun serializeElement(element: org.w3c.dom.Element): String {
        val transformer = TransformerFactory.newInstance().newTransformer()
        transformer.setOutputProperty(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "true")
        val writer = java.io.StringWriter()
        transformer.transform(DOMSource(element), StreamResult(writer))
        return writer.toString().trim()
    }

    private fun assertXmlOutput(
        inputMap: Map<String, String>,
        paramCatalog: ParamCatalog = emptyMap(),
        expectedXmlFragment: String,
        expectedErrors: List<String> = emptyList(),
    ) {
        val (_, resources) = createResourcesElement()
        val errors = mutableListOf<String>()

        mapToStringXml(
            map = inputMap,
            resources = resources,
            paramCatalog = paramCatalog,
            onError = { errors.add(it) }
        )
        val header = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        val actualXml = serializeElement(resources)
        assertEquals(header + expectedXmlFragment.trim(), actualXml)
        assertEquals(expectedErrors.toString(), errors.toString())
    }

    @Test
    fun `converts simple key-value pair to string element`() {
        val input = mapOf("hello" to "Hello World")
        assertXmlOutput(
            inputMap = input,
            expectedXmlFragment =
                """<resources>"""+
                """<string name="hello">Hello World</string>""" +
                """</resources>""",
        )
    }

    @Test
    fun `handles parameterized strings with catalog`() {
        val input = mapOf("welcome" to "Hello {name}")
        val catalog = mapOf(
            "welcome" to Translatable(paramList = listOf("name"), defaultValue = "Hello {name}")
        )
        assertXmlOutput(
            inputMap = input,
            paramCatalog = catalog,
            expectedXmlFragment =
                """<resources>""" +
                """<string name="welcome">Hello %1${'$'}s</string>""" +
                """</resources>""",
        )
    }

    @Test
    fun `escapes special characters properly`() {
        val input = mapOf("quote_test" to "He said \"Hello\"")
        assertXmlOutput(
            inputMap = input,
            expectedXmlFragment =
                """<resources>""" +
                """<string name="quote_test">He said \"Hello\"</string>""" +
                """</resources>""",
        )
    }

    @Test
    fun `handles multiple entries`() {
        val input = mapOf(
            "first" to "First string",
            "second" to "Second string",
        )
        assertXmlOutput(
            inputMap = input,
            expectedXmlFragment =
                """<resources>""" +
                """<string name="first">First string</string>""" +
                """<string name="second">Second string</string>""" +
                """</resources>""",
        )
    }

    @Test
    fun `handles missing catalog entry gracefully`() {
        val input = mapOf("simple" to "Simple string")
        assertXmlOutput(
            inputMap = input,
            paramCatalog = emptyMap(),
            expectedXmlFragment =
                """<resources>""" +
                """<string name="simple">Simple string</string>""" +
                """</resources>""",
        )
    }

    @Test
    fun `reports parameter mismatch error`() {
        val input = mapOf("test" to "Value with {missing_param}")
        val catalog = mapOf(
            "test" to Translatable(
                paramList = listOf("different_param"),
                defaultValue = "Default {different_param}",
            )
        )
        assertXmlOutput(
            inputMap = input,
            paramCatalog = catalog,
            expectedXmlFragment =
                """<resources>""" +
                """<string name="test">Value with {missing_param}</string>""" +
                """</resources>""",
            expectedErrors = listOf(
                """Key "test" has a parameter "missing_param" not present in the original string."""
            )
        )
    }
}

class AutoCompleteTranslationsTest {

    @Test
    fun `returns complete catalog when current map is empty`() {
        val currentMap = emptyMap<String, String>()
        val paramCatalog = mapOf(
            "key1" to Translatable(paramList = emptyList(), defaultValue = "default1"),
        )

        val result = autoCompleteTranslations(currentMap, paramCatalog)

        val expected = mapOf(
            "key1" to "default1",
        )
        assertEquals(expected, result)
    }

    @Test
    fun `uses current map values when available`() {
        val currentMap = mapOf(
            "key1" to "translated1",
        )
        val paramCatalog = mapOf(
            "key1" to Translatable(paramList = emptyList(), defaultValue = "default1"),
        )

        val result = autoCompleteTranslations(currentMap, paramCatalog)

        val expected = mapOf(
            "key1" to "translated1",
        )
        assertEquals(expected, result)
    }

    @Test
    fun `handles partial completion`() {
        val currentMap = mapOf(
            "key2" to "translated2",
        )
        val paramCatalog = mapOf(
            "key1" to Translatable(paramList = emptyList(), defaultValue = "default1"),
            "key2" to Translatable(paramList = emptyList(), defaultValue = "default2"),
        )

        val result = autoCompleteTranslations(currentMap, paramCatalog)

        val expected = mapOf(
            "key1" to "default1",
            "key2" to "translated2",
        )
        assertEquals(expected, result)
    }

    @Test
    fun `returns empty map when paramCatalog is empty`() {
        val currentMap = mapOf("extra" to "value")
        val paramCatalog = emptyMap<String, Translatable>()

        val result = autoCompleteTranslations(currentMap, paramCatalog)

        val expected = mapOf(
            "extra" to "value",
        )
        assertEquals(expected, result)
    }
}
