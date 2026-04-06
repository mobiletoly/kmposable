import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.vanniktechMavenPublish)
}

kotlin {
    jvmToolchain(17)
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    android {
        namespace = "dev.goquick.kmposable.navigation3"
        compileSdk = libs.versions.androidCompileSdk.get().toInt()
        minSdk = libs.versions.androidMinSdk.get().toInt()

        withJava()
        withHostTestBuilder {}.configure {}
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":library-core"))
                api(project(":library-compose"))
                api(libs.navigation3Ui)
                api(libs.navigation3LifecycleViewmodel)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlinTest)
                implementation(libs.kotlinxCoroutinesTest)
            }
        }
        val androidDeviceTest by getting {
            dependencies {
                implementation(libs.androidxActivityCompose)
                implementation(libs.androidxTestExt)
                implementation(libs.androidxTestRunner)
                implementation(libs.androidxTestCore)
                implementation(libs.composeRuntime)
                implementation(libs.composeMaterial3)
                implementation(libs.composeUiTestJunit4)
            }
        }
    }
}

mavenPublishing {
    publishToMavenCentral()

    signAllPublications()

    coordinates(group.toString(), "navigation3", version.toString())

    pom {
        name = "KMPosable Navigation 3 integration"
        description = "Navigation 3 KMP integration module for KMPosable"
        inceptionYear = "2026"
        url = "https://github.com/mobiletoly/kmposable/"
        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "repo"
            }
        }
        developers {
            developer {
                id = "mobiletoly"
                name = "Toly Pochkin"
                url = "https://github.com/mobiletoly"
            }
        }
        scm {
            url = "https://github.com/mobiletoly/kmposable"
            connection = "scm:git:git://github.com/mobiletoly/kmposable.git"
            developerConnection = "scm:git:git://github.com/mobiletoly/kmposable.git"
        }
    }
}

tasks.configureEach {
    if (name == "copyAndroidDeviceTestComposeResourcesToAndroidAssets") {
        val outputDirectoryGetter = javaClass.methods.first { it.name == "getOutputDirectory" }
        val outputDirectory = outputDirectoryGetter.invoke(this) as org.gradle.api.file.DirectoryProperty
        outputDirectory.set(layout.buildDirectory.dir("generated/compose/deviceTest/assets"))
    }
}
