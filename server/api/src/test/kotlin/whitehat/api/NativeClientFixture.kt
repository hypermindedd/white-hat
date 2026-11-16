package whitehat.api

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermissions
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.time.Instant
import java.util.Date
import java.util.UUID
import java.util.concurrent.CountDownLatch
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import whitehat.adapters.EnginePolicy
import whitehat.adapters.PostgresStore
import whitehat.application.RunWorker
import whitehat.domain.*

/** Test-only host for the existing native client's real HTTP lifecycle test. */
fun main(args: Array<String>) {
    require(args.size == 1) { "Supply a private fixture ready-file path" }
    val ready = Path.of(args.single())
    val pg = EmbeddedPostgres.builder().setPort(0).setServerConfig("unix_socket_directories", "").start()
    val source = pg.postgresDatabase
    val pair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    val signer = Algorithm.RSA256(pair.public as RSAPublicKey, pair.private as RSAPrivateKey)
    val issuer = "https://native-client-fixture.test"
    val audience = "whitehat-api"
    val verifier = JWT.require(Algorithm.RSA256(pair.public as RSAPublicKey, null))
        .withIssuer(issuer).withAudience(audience).withClaim("token_use", "access").build()
    PostgresStore.migrate(source)
    val store = PostgresStore(source, EnginePolicy().hash)
    val outputs = Json.parseToJsonElement(requireNotNull(object {}.javaClass.getResourceAsStream("/legacy-output-fixtures.json"))
        .use { it.readBytes().toString(Charsets.UTF_8) }).jsonObject
    val provider = object : DecisionProvider {
        override val configured = true
        override suspend fun analyze(decision: Decision) = ProviderResponse(
            outputs.getValue(decision.input.mode.wireId).toString(),
            Receipt("synthetic-client-fixture", "fixture-${UUID.randomUUID()}", 10, 10)
        )
    }
    val worker = RunWorker(store, provider)
    val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    val job = scope.launch { while (isActive) { if (!worker.tick()) delay(50) } }
    val server = embeddedServer(Netty, host = "127.0.0.1", port = 0) {
        whiteHatApi(store, verifier) { true }
    }.start(wait = false)
    val stopped = CountDownLatch(1)
    Runtime.getRuntime().addShutdownHook(Thread {
        runBlocking { job.cancelAndJoin() }
        scope.cancel()
        server.stop(0, 3000)
        pg.close()
        Files.deleteIfExists(ready)
        stopped.countDown()
    })
    val port = runBlocking { server.engine.resolvedConnectors().single().port }
    val token = JWT.create().withIssuer(issuer).withAudience(audience).withSubject("c5-runtime")
        .withClaim("token_use", "access").withExpiresAt(Date.from(Instant.now().plusSeconds(300))).sign(signer)
    val pending = ready.resolveSibling(ready.fileName.toString() + ".pending")
    Files.createFile(pending, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")))
    Files.writeString(pending, buildJsonObject {
        put("origin", "http://127.0.0.1:$port"); put("token", token)
    }.toString())
    Files.move(pending, ready, StandardCopyOption.ATOMIC_MOVE)
    stopped.await()
}
