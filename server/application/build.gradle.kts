plugins { kotlin("jvm") }
kotlin { jvmToolchain(21) }
dependencies {
    implementation(project(":server:domain"))
    implementation(project(":shared:contracts"))
    implementation(libs.json)
    implementation(libs.coroutines)
    testImplementation(kotlin("test-junit"))
}
