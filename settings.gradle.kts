rootProject.name = providers.gradleProperty("ARTIFACT_ID").get()
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

