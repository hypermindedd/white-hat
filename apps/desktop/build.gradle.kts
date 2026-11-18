import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    kotlin("plugin.compose")
    id("org.jetbrains.compose") version "1.9.0"
}

kotlin { jvmToolchain(21) }

dependencies {
    implementation(project(":shared:client"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material)
    implementation(libs.coroutines)
    implementation("net.java.dev.jna:jna-platform:5.17.0")
    testImplementation(kotlin("test"))
    implementation("io.ktor:ktor-client-cio:${libs.versions.ktor.get()}")
}

compose.desktop {
    application {
        mainClass = "whitehat.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Dmg, TargetFormat.Msi)
            packageName = "WhiteHat"
            packageVersion = providers.gradleProperty("whiteHatPackageVersion").getOrElse("1.0.0")
            modules("jdk.httpserver", "jdk.crypto.ec", "java.naming", "java.management", "java.sql", "java.net.http")
        }
    }
}
