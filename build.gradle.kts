plugins {
    // declare the versions ONCE here
    kotlin("jvm") version "2.3.20" apply false
    kotlin("multiplatform") version "2.3.20" apply false // if your sample will be KMP
    id("org.jetbrains.compose") version "1.10.3" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.20" apply false
    id("com.android.application") version "9.1.0" apply false
    id("com.android.library") version "9.1.0" apply false
    id("com.android.kotlin.multiplatform.library") version "9.1.0" apply false
    id("com.vanniktech.maven.publish") version "0.36.0" apply false
}

val kmposableGroup: String by project
val kmposableVersion: String by project

repositories {
    mavenCentral()
}


allprojects {
    group = kmposableGroup
    version = kmposableVersion

    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}
