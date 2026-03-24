pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

rootProject.name = "KMPosable"

include(
    ":library-core",
    ":library-compose",
    ":library-test",
    ":sample-app-compose",
    ":sample-app-compose:composeApp",
    ":sample-app-compose:androidApp",
    ":sample-app-flowscript",
    ":sample-app-flowscript:composeApp",
    ":sample-app-flowscript:androidApp"
)
