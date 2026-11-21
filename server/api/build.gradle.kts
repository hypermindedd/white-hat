plugins { kotlin("jvm"); application }
kotlin { jvmToolchain(21) }
application { mainClass.set("whitehat.api.MainKt") }
sourceSets.main { resources.srcDir(rootProject.file("contracts/openapi")) }
val postgresTestVersion = providers.gradleProperty("whiteHatTestPostgresVersion").orElse("16.15.0")
dependencies {
    implementation(project(":server:domain"))
    implementation(project(":server:application"))
    implementation(project(":server:adapters"))
    implementation(project(":shared:contracts"))
    implementation(libs.json)
    implementation(libs.coroutines)
    implementation(libs.hikari)
    for (artifact in listOf("core", "netty", "auth", "auth-jwt", "status-pages", "call-id", "default-headers")) {
        implementation("io.ktor:ktor-server-$artifact-jvm:${libs.versions.ktor.get()}")
    }
    runtimeOnly("org.slf4j:slf4j-simple:2.0.17")
    testImplementation(kotlin("test-junit"))
    testImplementation(libs.embedded.postgres)
    testImplementation(libs.flyway.core)
    testImplementation(enforcedPlatform("io.zonky.test.postgres:embedded-postgres-binaries-bom:${postgresTestVersion.get()}"))
    testImplementation(libs.postgres)
    testImplementation("io.ktor:ktor-server-test-host-jvm:${libs.versions.ktor.get()}")
}
tasks.test { testLogging { events("passed", "skipped", "failed"); showStandardStreams = false } }

tasks.register("writeNativeClientFixtureClasspath") {
    dependsOn(tasks.testClasses)
    val destination = layout.buildDirectory.file("native-client-fixture-classpath.txt")
    outputs.file(destination)
    doLast { destination.get().asFile.writeText(sourceSets.test.get().runtimeClasspath.asPath) }
}
