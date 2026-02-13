plugins {
    `kotlin-dsl`
    `maven-publish`
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
    plugins {
        create(pluginName) {
            id = "${group}.${artifactId}"
            implementationClass = "$group.${pluginName}Plugin"
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
        // Public publishing, ./gradlew publish
        maven {
            name = "GitHubPackages"
            url = uri(
                "https://maven.pkg.github.com/" +
                        "${githubUser}/" +
                        "${rootProject.name}"
            )
            credentials {
                username = "$githubUser"
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

