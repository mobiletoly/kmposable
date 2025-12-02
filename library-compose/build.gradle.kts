import com.android.build.api.dsl.androidLibrary
import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.vanniktechMavenPublish)
}

@OptIn(ExperimentalComposeLibrary::class)
kotlin {
    jvmToolchain(17)
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    androidLibrary {
        namespace = "dev.goquick.kmposable.compose"
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
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":library-core"))
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(libs.androidxLifecycleViewmodel)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlinTest)
                implementation(libs.kotlinxCoroutinesTest)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.kotlinxCoroutinesTest)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.androidxActivityCompose)
                implementation(libs.androidxLifecycleViewmodelCompose)
            }
        }
        val androidHostTest by getting {
            dependencies {
                implementation(libs.androidxTestExt)
                implementation(libs.androidxTestRunner)
                implementation(libs.kotlinTest)
                implementation(libs.kotlinxCoroutinesTest)
                implementation(compose.runtime)
                implementation(compose.material3)
                implementation(compose.uiTestJUnit4)
            }
        }
        val androidDeviceTest by getting {
            dependencies {
                implementation(libs.androidxTestExt)
                implementation(libs.androidxTestRunner)
                implementation(libs.androidxTestCore)
                implementation(compose.runtime)
                implementation(compose.material3)
                implementation(compose.uiTestJUnit4)
            }
        }
    }
}

mavenPublishing {
    publishToMavenCentral()

    signAllPublications()

    coordinates(group.toString(), "compose", version.toString())

    pom {
        name = "KMPosable Compose adapter"
        description = "Compose Multiplatform adapter for KMPosable"
        inceptionYear = "2025"
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
