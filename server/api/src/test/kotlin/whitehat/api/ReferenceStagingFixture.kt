package whitehat.api

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.postgresql.ds.PGSimpleDataSource
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermissions
import java.util.UUID
import java.util.concurrent.CountDownLatch
import whitehat.adapters.EnginePolicy
import whitehat.adapters.PostgresStore
import whitehat.application.RunWorker
import whitehat.domain.*

/** Isolated bootstrap only: all HTTP, permissions and writes use the actual application. */
fun main(args: Array<String>) {
    require(args.size == 1)
    val issuer = requireNotNull(System.getenv("WH_S2_ISSUER"))
    val subject = requireNotNull(System.getenv("WH_S2_SUBJECT"))
    val ready = Path.of(args.single())
    val pg = EmbeddedPostgres.builder().setPort(0).setServerConfig("unix_socket_directories", "").start()
    val owner = pg.postgresDatabase
    PostgresStore.migrate(owner)
    val password = UUID.randomUUID().toString().replace("-", "")
    owner.connection.use { c -> c.createStatement().use { s ->
        s.execute("CREATE ROLE wh_reference_app LOGIN NOSUPERUSER NOBYPASSRLS PASSWORD '$password'")
        s.execute("GRANT USAGE ON SCHEMA public TO wh_reference_app")
        s.execute("GRANT SELECT,INSERT,UPDATE,DELETE ON ALL TABLES IN SCHEMA public TO wh_reference_app")
        s.execute("CREATE DATABASE wh_reference_sessions")
    } }
    val appSource = PGSimpleDataSource().apply {
        setURL("jdbc:postgresql://127.0.0.1:${pg.port}/postgres"); user = "wh_reference_app"; this.password = password
    }
    val store = PostgresStore(appSource, EnginePolicy().hash)
    val workspace = store.createWorkspace(subject, "S2 reference workspace")
    val outputs = Json.parseToJsonElement(requireNotNull(object {}.javaClass.getResourceAsStream("/legacy-output-fixtures.json"))
        .use { it.readBytes().toString(Charsets.UTF_8) }).jsonObject
    val provider = object : DecisionProvider {
        override val configured = true
        override suspend fun analyze(decision: Decision) = ProviderResponse(outputs.getValue(decision.input.mode.wireId).toString(),
            Receipt("synthetic-reference-only", "synthetic-${UUID.randomUUID()}", 10, 10))
    }
    val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    val worker = RunWorker(store, provider)
    scope.launch { while (isActive) { if (!worker.tick()) delay(100) } }
    val verifier = accessIdentityVerifier(issuer, "$issuer/protocol/openid-connect/certs", "whitehat-api")
    val voicePilot = VoicePilot(store, workspace.id) {
        object : TranscriptionTurn {
            private var received = 0
            override fun append(pcm: ByteArray) { received += pcm.size }
            override fun finish(): String {
                check(received >= 4_800)
                return "متن ساختگی وایت‌هت برای آزمون میکروفن"
            }
            override fun close() = Unit
        }
    }
    val server = embeddedServer(Netty, host = "127.0.0.1", port = 0) {
        whiteHatApi(store, verifier, voicePilot) { true }
    }.start(wait = false)
    val stopped = CountDownLatch(1)
    Runtime.getRuntime().addShutdownHook(Thread {
        scope.cancel(); server.stop(0, 3000); voicePilot.close(); pg.close(); Files.deleteIfExists(ready); stopped.countDown()
    })
    val port = runBlocking { server.engine.resolvedConnectors().single().port }
    val pending = ready.resolveSibling(ready.fileName.toString() + ".pending")
    Files.createFile(pending, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")))
    Files.writeString(pending, buildJsonObject {
        put("api", "http://127.0.0.1:$port"); put("workspace", workspace.id.toString()); put("subject", subject)
        put("adminDb", "postgresql://postgres@127.0.0.1:${pg.port}/postgres")
        put("appDb", "postgresql://wh_reference_app:$password@127.0.0.1:${pg.port}/postgres")
        put("sessionAdminDb", "postgresql://postgres@127.0.0.1:${pg.port}/wh_reference_sessions")
    }.toString())
    Files.move(pending, ready, StandardCopyOption.ATOMIC_MOVE)
    stopped.await()
}
