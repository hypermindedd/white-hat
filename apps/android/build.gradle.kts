plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("plugin.compose")
}

android {
    namespace = "whitehat.android"
    compileSdk = 35
    buildFeatures { buildConfig = true }
    defaultConfig {
        applicationId = "whitehat.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        val host = providers.gradleProperty("whiteHatAndroidOidcHost").getOrElse("whitehat.example.invalid")
        require(host.matches(Regex("[a-zA-Z0-9.-]+")))
        manifestPlaceholders["oidcHost"] = host
        buildConfigField("String", "REDIRECT", "\"https://$host/oauth/android\"")
        for (key in listOf("API", "ISSUER", "CLIENT_ID", "AUTHORIZATION_URL", "TOKEN_URL", "REVOCATION_URL", "JWKS_URL")) {
            val value = providers.gradleProperty("whiteHatAndroid_$key").getOrElse("")
            require(value.none { it == '"' || it == '\\' || it.isISOControl() })
            buildConfigField("String", key, "\"$value\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
}

dependencies {
    implementation(project(":apps:android:client"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.material:material:1.8.2")
    implementation(libs.coroutines)
    androidTestImplementation("androidx.test:runner:1.6.2")
    implementation("io.ktor:ktor-client-okhttp:${libs.versions.ktor.get()}")
}
