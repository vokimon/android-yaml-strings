plugins {
    `kotlin-dsl`
    `maven-publish`
}

group = "net.canvoki"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.2")
    implementation(gradleApi())

    testImplementation("junit:junit:4.13.2")
    testImplementation(gradleTestKit())
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.0")

}

gradlePlugin {
    plugins {
        create("androidYamlStrings") {
            id = "net.canvoki.android-yaml-strings"
            implementationClass = "net.canvoki.gradle.YamlToAndroidPlugin"
        }
    }
}

