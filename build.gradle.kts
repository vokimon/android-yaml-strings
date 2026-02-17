plugins {
    `kotlin-dsl`
    `maven-publish`
    id("com.gradle.plugin-publish") version "2.0.0"
}

group = property("GROUP") as String
version = property("VERSION") as String

val pluginId = property("PLUGIN_ID") as String
val pluginName = property("PLUGIN_NAME") as String
val pluginClass = property("PLUGIN_CLASS") as String
val githubUrl = property("GITHUB_URL") as String
val summary = property("SUMMARY") as String
val description = property("DESCRIPTION") as String
val pluginTags: List<String> = providers
    .gradleProperty("TAGS")
    .map { it.split(",").map(String::trim).filter(String::isNotEmpty) }
    .orElse(emptyList())
    .get()

dependencies {
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.2")
    implementation(gradleApi())
    compileOnly("com.android.tools.build:gradle-api:8.13.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation(gradleTestKit())
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.0")
    testImplementation("io.github.java-diff-utils:java-diff-utils:4.12")
}

gradlePlugin {
    // Public publish: ./gradlew publishPlugins
    // Define envs GRADLE_PUBLISH_KEY and GRADLE_PUBLISH_SECRET
    plugins {
        create(pluginName) {
            id = pluginId
            implementationClass = pluginClass
            displayName = summary
            description = description
            website = githubUrl
            vcsUrl = githubUrl
            tags.set(pluginTags)
        }
    }
}

// Testing configuration
tasks.withType<Test> {
    testLogging {
        events("failed", "skipped")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = true
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
}
