buildscript {
    // Kotlin's Android target needs the Android Gradle plugin on the shared classpath.
    if (providers.gradleProperty("whiteHatAndroid").orNull == "true") {
        repositories { google(); mavenCentral() }
        dependencies { classpath("com.android.tools.build:gradle:8.11.1") }
    }
}

plugins {
    kotlin("multiplatform") version "2.2.20" apply false
    kotlin("jvm") version "2.2.20" apply false
    kotlin("plugin.serialization") version "2.2.20" apply false
    kotlin("plugin.compose") version "2.2.20" apply false
}
