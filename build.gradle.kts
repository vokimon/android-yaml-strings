plugins {
    `kotlin-dsl`
    `maven-publish`
    id("com.gradle.plugin-publish") version "2.0.0"
}

group = property("GROUP") as String
version = property("VERSION") as String

val pluginName: String = property("PLUGIN_NAME") as String
val artifactId: String = property("ARTIFACT_ID") as String
val githubUser: String = property("GITHUB_USER") as String

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.2")
    implementation(gradleApi())
    compileOnly("com.android.tools.build:gradle-api:8.13.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation(gradleTestKit())
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.0")
}

gradlePlugin {
    // Public publish
    // ./gradlew publishPlugins
    // Define envs GRADLE_PUBLISH_KEY and GRADLE_PUBLISH_SECRET
    plugins {
        create(pluginName) {
            id = "$group.$artifactId"
            implementationClass = "$group.${pluginName}Plugin"
            displayName = "Android YAML Strings Plugin"
            description = "Generates Android strings.xml files from YAML translation files."
            website = "https://github.com/$githubUser/$artifactId"
            vcsUrl = "https://github.com/$githubUser/$artifactId"
            tags = listOf("android", "yaml", "strings", "translations")
        }
    }
}


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


publishing {
    repositories {
        mavenLocal() // For local testing ./gradlew publishToMavenLocal
    }
}
