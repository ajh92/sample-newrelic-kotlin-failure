rootProject.name = "sample-newrelic-kotlin-failure"
val micronautVersion: String by settings
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    versionCatalogs {
        create("mn") {
            from("io.micronaut.platform:micronaut-platform:$micronautVersion")
        }
    }
}
