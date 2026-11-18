import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    jvmToolchain(21)
    jvm {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_1_8)
    }
    val framework = XCFramework("WhiteHatClient")
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "WhiteHatClient"
            isStatic = true
            framework.add(this)
        }
    }

    sourceSets {
        commonMain.dependencies {
            api("io.ktor:ktor-client-core:${libs.versions.ktor.get()}")
            api(libs.json)
            implementation(libs.coroutines)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("io.ktor:ktor-client-mock:${libs.versions.ktor.get()}")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${libs.versions.coroutines.get()}")
        }
        jvmMain { kotlin.srcDir("src/jvmAuth/kotlin") }
        jvmMain.dependencies {
            implementation("com.auth0:java-jwt:4.5.0")
            implementation("com.auth0:jwks-rsa:0.23.0")
        }
        iosMain.dependencies {
            implementation("io.ktor:ktor-client-darwin:${libs.versions.ktor.get()}")
        }
        jvmTest.dependencies {
            implementation("io.ktor:ktor-client-cio:${libs.versions.ktor.get()}")
        }
    }
}

tasks.named<Test>("jvmTest") {
    System.getenv("WH_C5_TEST_TRUSTSTORE")?.let {
        systemProperty("javax.net.ssl.trustStore", it)
        systemProperty("javax.net.ssl.trustStorePassword", "changeit")
    }
    testLogging { events("passed", "skipped", "failed") }
}
