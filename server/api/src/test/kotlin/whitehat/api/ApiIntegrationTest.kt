package whitehat.api

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.junit.Before
import org.junit.BeforeClass
import org.junit.AfterClass
import org.postgresql.ds.PGSimpleDataSource
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.time.Instant
import java.util.Date
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.Callable
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.*
import whitehat.adapters.*
import whitehat.application.RunWorker
import whitehat.contracts.Mode
import whitehat.domain.*

class ApiIntegrationTest {
    companion object {
        private lateinit var pg: EmbeddedPostgres
        private val pair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        private val signer = Algorithm.RSA256(pair.public as RSAPublicKey, pair.private as RSAPrivateKey)
        private val verifier = JWT.require(Algorithm.RSA256(pair.public as RSAPublicKey, null)).withIssuer("https://issuer.test").withAudience("whitehat-api").build()
        @JvmStatic @BeforeClass fun startDatabase() { pg = EmbeddedPostgres.builder().setPort(0).setServerConfig("unix_socket_directories", "").start() }
        @JvmStatic @AfterClass fun stopDatabase() { if (::pg.isInitialized) pg.close() }
    }
    private lateinit var source: PGSimpleDataSource
    private lateinit var store: PostgresStore
    private lateinit var workspace: Workspace
    private val policy = EnginePolicy()
    private val validOutputs = Json.parseToJsonElement(requireNotNull(javaClass.getResourceAsStream("/legacy-output-fixtures.json")).use { it.readBytes().toString(Charsets.UTF_8) }).jsonObject

    @Before fun prepare() {
        val name = "test_" + UUID.randomUUID().toString().replace("-", "")
        pg.postgresDatabase.connection.use { it.createStatement().use { s -> s.execute("CREATE DATABASE $name") } }
        source = PGSimpleDataSource().apply { setURL("jdbc:postgresql://127.0.0.1:${pg.port}/$name"); user = "postgres"; password = "postgres" }
        assertEquals(1, PostgresStore.migrate(source))
        store = PostgresStore(source, policy.hash)
        workspace = store.createWorkspace("alice", "تیم ما")
    }
    private fun token(subject: String = "alice", audience: String = "whitehat-api", kind: String = "access", expires: Boolean = true): String {
        val builder = JWT.create().withIssuer("https://issuer.test").withAudience(audience).withSubject(subject).withClaim("token_use", kind)
        if (expires) builder.withExpiresAt(Date.from(Instant.now().plusSeconds(300)))
        return builder.sign(signer)
    }
    private suspend fun HttpClient.requestJson(method: HttpMethod, path: String, actor: String? = "alice", body: String? = null, key: String? = null): HttpResponse = request(path) {
        this.method = method
        if (actor != null) bearerAuth(token(actor))
        if (body != null) { contentType(ContentType.Application.Json); setBody(body) }
        if (key != null) header("Idempotency-Key", key)
    }
    private suspend fun HttpResponse.obj() = Json.parseToJsonElement(bodyAsText()).jsonObject
    private fun path(suffix: String = "") = "/api/v1/workspaces/${workspace.id}$suffix"
    private fun draft(mode: Mode = Mode.CHALLENGE) = store.createDecision("alice", workspace.id, DecisionInput("تصمیم", "آیا پایلوت اجرا شود؟", "سنجش پرداخت", mode = mode))
    private fun output(mode: Mode) = validOutputs.getValue(mode.wireId).toString()
    private inner class Fixture : DecisionProvider {
        override val configured = true
        val calls = AtomicInteger()
        var action: suspend (Decision) -> String = { output(it.input.mode) }
        override suspend fun analyze(decision: Decision): ProviderResponse {
            calls.incrementAndGet()
            return ProviderResponse(action(decision), Receipt("fixture-not-live", "fixture-receipt", 100, 80))
        }
    }

    @Test fun migrationIsRepeatableAndAuthCannotBeBypassed() = testApplication {
        assertEquals(0, PostgresStore.migrate(source))
        PostgresStore.validate(source)
        application { whiteHatApi(store, verifier) { true } }
        assertEquals(HttpStatusCode.OK, client.get("/api/v1/health").status)
        val noAuth = client.get("/api/v1/me")
        assertEquals(HttpStatusCode.Unauthorized, noAuth.status)
        assertNotNull(noAuth.headers["X-Request-ID"])
        for (jwt in listOf(token(audience = "wrong"), token(kind = "id"), token(expires = false), "not.a.token")) {
            assertEquals(HttpStatusCode.Unauthorized, client.get("/api/v1/me") { bearerAuth(jwt) }.status)
        }
        val expired = JWT.create().withIssuer("https://issuer.test").withAudience("whitehat-api").withSubject("alice").withClaim("token_use", "access").withExpiresAt(Date.from(Instant.now().minusSeconds(60))).sign(signer)
        val wrongAlgorithm = JWT.create().withIssuer("https://issuer.test").withAudience("whitehat-api").withSubject("alice").withClaim("token_use", "access").withExpiresAt(Date.from(Instant.now().plusSeconds(60))).sign(Algorithm.HMAC256("fixture-only-not-a-key"))
        for (jwt in listOf(expired, wrongAlgorithm)) assertEquals(HttpStatusCode.Unauthorized, client.get("/api/v1/me") { bearerAuth(jwt) }.status)
        assertEquals("alice", client.requestJson(HttpMethod.Get, "/api/v1/me").obj().getValue("subject").jsonPrimitive.content)
    }

    @Test fun actualHttpRouteQueuesAndWorkerCompletesAllSevenModes() = testApplication {
        application { whiteHatApi(store, verifier) { true } }
        val fixture = Fixture(); val worker = RunWorker(store, fixture)
        for (mode in Mode.entries) {
            val create = client.requestJson(HttpMethod.Post, path("/decisions"), body = """{"title":"پایلوت","decision":"پایلوت را اجرا کنیم؟","objective":"سنجش پرداخت","mode":"${mode.wireId}"}""")
            assertEquals(HttpStatusCode.Created, create.status)
            val id = create.obj().getValue("id").jsonPrimitive.content
            val queued = client.requestJson(HttpMethod.Post, path("/decisions/$id/runs"), body = """{"expected_revision":1}""", key = "test_${mode.wireId}_0001")
            assertEquals(HttpStatusCode.Accepted, queued.status)
            val run = queued.obj().getValue("id").jsonPrimitive.content
            assertTrue(worker.tick())
            val result = client.requestJson(HttpMethod.Get, path("/runs/$run")).obj()
            val sample = java.io.File("build/contract-samples/run-${mode.wireId}.json")
            sample.parentFile.mkdirs(); sample.writeText(result.toString())
            assertEquals("succeeded", result.getValue("status").jsonPrimitive.content)
            assertEquals(mode.wireId, result.getValue("result").jsonObject.getValue("mode").jsonPrimitive.content)
            assertEquals(100, result.getValue("input_tokens").jsonPrimitive.int)
            assertEquals("no-store", client.requestJson(HttpMethod.Get, path("/runs/$run")).headers["Cache-Control"])
        }
        assertEquals(7, fixture.calls.get())
        assertEquals("3.1.0", client.get("/api/v1/openapi.json").obj().getValue("openapi").jsonPrimitive.content)
    }

    @Test fun organizationMemberSupportAndOwnerBoundariesAreEnforced() = testApplication {
        val decision = draft()
        store.setMember("alice", workspace.id, "bob", Role.MEMBER, true)
        store.setMember("alice", workspace.id, "support", Role.SUPPORT, true)
        val outside = store.createWorkspace("mallory", "دیگر")
        application { whiteHatApi(store, verifier) { true } }
        assertEquals(HttpStatusCode.NotFound, client.requestJson(HttpMethod.Get, path("/decisions/${decision.id}"), "bob").status)
        assertEquals(HttpStatusCode.NotFound, client.requestJson(HttpMethod.Get, path("/decisions"), "mallory").status)
        assertEquals(HttpStatusCode.NotFound, client.requestJson(HttpMethod.Get, "/api/v1/workspaces/${outside.id}/decisions").status)
        assertEquals(HttpStatusCode.Forbidden, client.requestJson(HttpMethod.Get, path("/decisions"), "support").status)
        assertEquals(HttpStatusCode.Forbidden, client.requestJson(HttpMethod.Get, path("/admin/usage"), "bob").status)
        assertEquals(HttpStatusCode.Forbidden, client.requestJson(HttpMethod.Post, path("/members"), "bob", """{"subject":"bob","role":"ADMIN","active":true}""").status)
        store.setMember("alice", workspace.id, "bob", Role.MEMBER, false)
        assertEquals(HttpStatusCode.NotFound, client.requestJson(HttpMethod.Get, path("/decisions"), "bob").status)
        val fail = assertFailsWith<Fault> { store.setMember("alice", workspace.id, "alice", Role.ADMIN, false) }
        assertEquals("OWNER_TRANSFER_REQUIRED", fail.code)
    }

    @Test fun revisionsAndIdempotencyAreBoundToAnImmutableInput() = testApplication {
        val d = draft(); val fixture = Fixture()
        application { whiteHatApi(store, verifier) { true } }
        val edited = client.requestJson(HttpMethod.Put, path("/decisions/${d.id}"), body = """{"expected_revision":1,"title":"بازنگری","decision":"آزمون جدید؟"}""")
        assertEquals(2, edited.obj().getValue("revision").jsonPrimitive.int)
        assertEquals(HttpStatusCode.Conflict, client.requestJson(HttpMethod.Post, path("/decisions/${d.id}/runs"), body = """{"expected_revision":1}""", key = "stale_key_001").status)
        val r = store.enqueue("alice", workspace.id, d.id, 2, "repeat_key_001")
        assertEquals(r.id, store.enqueue("alice", workspace.id, d.id, 2, "repeat_key_001").id)
        assertEquals("IDEMPOTENCY_CONFLICT", assertFailsWith<Fault> { store.enqueue("alice", workspace.id, d.id, 1, "repeat_key_001") }.code)
        assertEquals("ACTIVE_RUN_EXISTS", assertFailsWith<Fault> { store.editDecision("alice", workspace.id, d.id, 2, d.input) }.code)
        assertTrue(RunWorker(store, fixture).tick()); assertEquals(1, fixture.calls.get())
    }

    @Test fun concurrentDuplicatesCreateOneReservationAndOneProviderCall() = runBlocking {
        val d = draft(); val pool = Executors.newFixedThreadPool(8)
        try {
            val results = pool.invokeAll(List(8) { Callable { store.enqueue("alice", workspace.id, d.id, 1, "concurrent_0001").id } }).map { it.get() }
            assertEquals(1, results.toSet().size)
            val fixture = Fixture(); val worker = RunWorker(store, fixture)
            coroutineScope { listOf(async { worker.tick() }, async { worker.tick() }).awaitAll() }
            assertEquals(1, fixture.calls.get())
            source.connection.use { c -> c.createStatement().executeQuery("SELECT attempts FROM usage_counters").use { it.next(); assertEquals(1, it.getInt(1)) } }
        } finally { pool.shutdownNow() }
    }

    @Test fun quotaReservationsRemainAtomicAcrossDifferentDecisions() {
        val limited = PostgresStore(source, policy.hash, 2)
        val drafts = List(8) { draft() }; val pool = Executors.newFixedThreadPool(8)
        try {
            val results = pool.invokeAll(drafts.mapIndexed { index, d -> Callable {
                try { limited.enqueue("alice", workspace.id, d.id, 1, "quota_key_$index"); 202 } catch (f: Fault) { f.status }
            } }).map { it.get() }
            assertEquals(2, results.count { it == 202 }); assertEquals(6, results.count { it == 429 })
            assertEquals(2L, store.adminUsage("alice", workspace.id)["runs"])
            assertNull(store.adminUsage("alice", workspace.id)["input_tokens"])
        } finally { pool.shutdownNow() }
    }

    @Test fun invalidOutputFailsButKeepsActualUsage() = runBlocking {
        val d = draft(); val r = store.enqueue("alice", workspace.id, d.id, 1, "invalid_output_001")
        val fixture = Fixture().apply { action = { "{}" } }
        RunWorker(store, fixture).tick()
        val result = store.run("alice", workspace.id, r.id)
        assertEquals("failed", result.status); assertEquals("INVALID_MODEL_OUTPUT", result.error)
        assertNull(result.result); assertEquals(100L, result.inputTokens)
    }

    @Test fun uncertainProviderOutcomeIsNeverAutomaticallyRetried() = runBlocking {
        val d = draft(); val r = store.enqueue("alice", workspace.id, d.id, 1, "uncertain_001")
        val fixture = Fixture().apply { action = { throw ProviderFailure("PROVIDER_OUTCOME_UNKNOWN", true) } }
        val worker = RunWorker(store, fixture)
        worker.tick(); assertFalse(worker.tick())
        assertEquals("reconciliation_required", store.run("alice", workspace.id, r.id).status)
        assertEquals("reconciliation_required", store.enqueue("alice", workspace.id, d.id, 1, "uncertain_001").status)
        assertEquals("ACTIVE_RUN_EXISTS", assertFailsWith<Fault> { store.enqueue("alice", workspace.id, d.id, 1, "uncertain_002") }.code)
        assertEquals(1, fixture.calls.get())
    }

    @Test fun expiredLeaseRejectsLateOutputAndPreservesItsReceiptOnce() {
        val d = draft(); val r = store.enqueue("alice", workspace.id, d.id, 1, "lease_expiry_001")
        val claim = store.claim()!!
        source.connection.use { c -> c.createStatement().use { it.executeUpdate("UPDATE runs SET lease_until=now()-interval '1 second'") } }
        assertNull(store.claim())
        val completion = Completion(output(d.input.mode), receipt = Receipt("fixture-not-live", "late-receipt", 21, 13))
        store.complete(claim, completion); store.complete(claim, completion)
        val result = store.run("alice", workspace.id, r.id)
        assertEquals("reconciliation_required", result.status); assertNull(result.result); assertEquals(21L, result.inputTokens)
        source.connection.use { c -> c.createStatement().executeQuery("SELECT count(*) FROM provider_receipts").use { it.next(); assertEquals(1, it.getInt(1)) } }
    }

    @Test fun queuedAndInFlightCancellationsDiscardOutputWithoutHidingCost() = runBlocking {
        val d = draft(); val fixture = Fixture(); val worker = RunWorker(store, fixture)
        val r = store.enqueue("alice", workspace.id, d.id, 1, "cancel_queued_001")
        assertEquals("cancelled", store.cancel("alice", workspace.id, r.id).status)
        assertEquals("cancelled", store.cancel("alice", workspace.id, r.id).status)
        assertFalse(worker.tick()); assertEquals(0, fixture.calls.get())
        val started = CompletableDeferred<Unit>(); val finish = CompletableDeferred<Unit>()
        fixture.action = { started.complete(Unit); finish.await(); output(it.input.mode) }
        val running = store.enqueue("alice", workspace.id, d.id, 1, "cancel_running_001")
        val job = async { worker.tick() }; started.await()
        assertEquals("cancellation_requested", store.cancel("alice", workspace.id, running.id).status)
        finish.complete(Unit); job.await()
        val result = store.run("alice", workspace.id, running.id)
        assertEquals("cancelled", result.status); assertNull(result.result); assertEquals(100L, result.inputTokens)
    }

    @Test fun noProviderOrInvalidInputsCannotConsumeRunsOrInventAResult() = testApplication {
        val d = draft(); application { whiteHatApi(store, verifier) { false } }
        assertEquals(HttpStatusCode.Conflict, client.requestJson(HttpMethod.Post, path("/decisions/${d.id}/runs"), body = """{"expected_revision":1}""", key = "no_provider_001").status)
        assertEquals(0L, store.adminUsage("alice", workspace.id)["runs"])
        assertEquals(HttpStatusCode.BadRequest, client.requestJson(HttpMethod.Post, path("/decisions"), body = """{"title":"x","decision":"x","mode":"negotiation"}""").status)
        assertEquals(HttpStatusCode.BadRequest, client.requestJson(HttpMethod.Post, path("/decisions"), body = """{"title":"x","decision":"x","owner":"mallory"}""").status)
        assertEquals(HttpStatusCode.PayloadTooLarge, client.requestJson(HttpMethod.Post, path("/decisions"), body = "x".repeat(140001)).status)
    }

    @Test fun persistedStateAndAppendOnlyAuditSurviveRepositoryReconstruction() {
        val d = draft(); val r = store.enqueue("alice", workspace.id, d.id, 1, "persistent_001")
        val reopened = PostgresStore(source, policy.hash)
        assertEquals(d, reopened.decision("alice", workspace.id, d.id))
        assertEquals(r.id, reopened.run("alice", workspace.id, r.id).id)
        assertFails { source.connection.use { it.createStatement().executeUpdate("DELETE FROM audit") } }
        val compare = store.createDecision("alice", workspace.id, DecisionInput("مقایسه", "کدام؟", mode = Mode.COMPARE))
        assertEquals("COMPARE_OBJECTIVE_REQUIRED", assertFailsWith<Fault> { store.enqueue("alice", workspace.id, compare.id, 1, "compare_001") }.code)
    }

    @Test fun recoveryRunsEvenWithoutProviderConfiguration() = runBlocking {
        val d = draft(); val r = store.enqueue("alice", workspace.id, d.id, 1, "recover_no_provider_001")
        store.claim()
        source.connection.use { c -> c.createStatement().use { it.executeUpdate("UPDATE runs SET lease_until=now()-interval '1 second'") } }
        val absent = object : DecisionProvider {
            override val configured = false
            override suspend fun analyze(decision: Decision): ProviderResponse = error("Must not execute")
        }
        assertFalse(RunWorker(store, absent).tick())
        assertEquals("reconciliation_required", store.run("alice", workspace.id, r.id).status)
    }

    @Test fun revokedMemberCannotTriggerAPreviouslyQueuedProviderCall() = runBlocking {
        store.setMember("alice", workspace.id, "bob", Role.MEMBER, true)
        val d = store.createDecision("bob", workspace.id, DecisionInput("پایلوت", "آزمون اجرا شود؟"))
        store.enqueue("bob", workspace.id, d.id, 1, "revoked_member_001")
        store.setMember("alice", workspace.id, "bob", Role.MEMBER, false)
        val fixture = Fixture(); assertFalse(RunWorker(store, fixture).tick()); assertEquals(0, fixture.calls.get())
        source.connection.use { c -> c.createStatement().executeQuery("SELECT error_code FROM runs").use { it.next(); assertEquals("MEMBERSHIP_REVOKED", it.getString(1)) } }
    }

    @org.junit.Test(timeout = 20000) fun nettyServesAuthenticatedApiOverActualLoopbackHttp() {
        val server = embeddedServer(Netty, host = "127.0.0.1", port = 0) { whiteHatApi(store, verifier) { false } }.start(wait = false)
        try {
            val port = runBlocking { server.engine.resolvedConnectors().single().port }
            java.net.http.HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(5)).build().use { http ->
                val request = java.net.http.HttpRequest.newBuilder(java.net.URI("http://127.0.0.1:$port/api/v1/me")).timeout(java.time.Duration.ofSeconds(5)).header("Authorization", "Bearer ${token()}").GET().build()
                val response = http.send(request, java.net.http.HttpResponse.BodyHandlers.ofString())
                assertEquals(200, response.statusCode())
                assertEquals("alice", Json.parseToJsonElement(response.body()).jsonObject.getValue("subject").jsonPrimitive.content)
            }
        } finally { server.stop(0, 3000) }
    }
}
