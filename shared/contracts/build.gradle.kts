plugins { kotlin("multiplatform") }

kotlin {
    jvmToolchain(21)
    jvm()
    js(IR) { nodejs() }
    // Android/iOS consumers are enabled with their application toolchains later.
    // commonMain intentionally contains no platform or server implementation.
    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
        }
        commonTest.dependencies { implementation(kotlin("test")) }
    }
}
