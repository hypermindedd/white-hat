plugins { kotlin("jvm") }
kotlin { jvmToolchain(21) }
dependencies {
    implementation(project(":server:domain"))
    implementation(project(":shared:contracts"))
    implementation(libs.json)
    implementation(libs.postgres)
    implementation(libs.hikari)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgres)
    implementation("io.ktor:ktor-client-core-jvm:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-client-cio-jvm:${libs.versions.ktor.get()}")
    testImplementation(kotlin("test-junit"))
    testImplementation("io.ktor:ktor-client-mock-jvm:${libs.versions.ktor.get()}")
}
