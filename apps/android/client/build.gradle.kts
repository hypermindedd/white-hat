plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("plugin.serialization")
}

android {
    namespace = "whitehat.android.client"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    sourceSets.named("main") {
        kotlin.srcDir(rootProject.file("shared/client/src/commonMain/kotlin"))
        kotlin.srcDir(rootProject.file("shared/client/src/jvmAuth/kotlin"))
    }
}

dependencies {
    api("io.ktor:ktor-client-core:${libs.versions.ktor.get()}")
    api(libs.json)
    implementation(libs.coroutines)
    implementation("com.auth0:java-jwt:4.5.0")
    implementation("com.auth0:jwks-rsa:0.23.0")
}
