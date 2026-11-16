package whitehat.client

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull

private const val WORKSPACE = "00000000-0000-0000-0000-000000000001"
private const val DECISION = "00000000-0000-0000-0000-000000000002"
private const val RUN = "00000000-0000-0000-0000-000000000003"
private val PREVIOUS = NativeTokens("owner", "old", "refresh")
private val ROTATED = NativeTokens("owner", "new", "rotated")

private class TestIdentity(var next: NativeTokens? = ROTATED) : NativeIdentity, NativeTokenStore {
    var stored: NativeTokens? = PREVIOUS
    var refreshes = 0
    var remoteFails = false
    var revoked: NativeTokens? = null

    override suspend fun read() = stored
    override suspend fun write(tokens: NativeTokens?) { stored = tokens }
    override suspend fun signIn() = PREVIOUS
    override suspend fun refresh(current: NativeTokens): NativeTokens? {
        refreshes++
        return next
    }
    override suspend fun revoke(current: NativeTokens) {
        revoked = current
        if (remoteFails) error("Revocation offline")
    }
}

class WhiteHatClientTest {
    @Test
    fun refreshesReadAndPersistsRotatedTokens() = runTest {
        val auth = TestIdentity()
        val engine = MockEngine { req ->
            if (req.headers[HttpHeaders.Authorization] == "Bearer old") {
                respond("""{"error":"AUTH_REQUIRED","request_id":"request"}""", HttpStatusCode.Unauthorized)
            } else {
                assertEquals("Bearer new", req.headers[HttpHeaders.Authorization])
                respond("""{"subject":"owner","workspaces":[]}""")
            }
        }
        WhiteHatClient("https://api.example.test/api/v1", engine, NativeSession(auth, auth)).use {
            assertEquals("owner", it.me().subject)
            assertEquals(1, auth.refreshes)
            assertEquals("rotated", auth.stored?.refreshToken)
        }
        engine.close()
    }

    @Test
    fun refreshesRejectedTokenOnlyOnceAndNeverUsesAnotherAccount() = runTest {
        val auth = TestIdentity()
        val session = NativeSession(auth, auth)
        val a = async { session.refreshAfter(PREVIOUS) }
        val b = async { session.refreshAfter(PREVIOUS) }
        assertEquals(ROTATED, a.await())
        assertEquals(ROTATED, b.await())
        assertEquals(1, auth.refreshes)
        auth.stored = NativeTokens("different", "different-token", null)
        assertNull(session.refreshAfter(PREVIOUS))
        assertEquals("different", auth.stored?.subject)
    }

    @Test
    fun refreshDoesNotAutomaticallyReplayMutation() = runTest {
        val auth = TestIdentity()
        var calls = 0
        val engine = MockEngine {
            calls++
            respond("""{"error":"AUTH_REQUIRED"}""", HttpStatusCode.Unauthorized)
        }
        WhiteHatClient("https://api.example.test/api/v1", engine, NativeSession(auth, auth)).use {
            val error = assertFailsWith<ApiFailure> {
                it.enqueue(RunSubmission(WORKSPACE, DECISION, 1, "client-attempt"))
            }
            assertEquals(Recovery.RETRY_AFTER_REFRESH, error.recovery)
            assertEquals(1, calls)
            assertEquals(1, auth.refreshes)
        }
        engine.close()
    }

    @Test
    fun rejectedRefreshClearsSessionAndLogoutClearsBeforeRemoteFailure() = runTest {
        val auth = TestIdentity(next = null)
        val session = NativeSession(auth, auth)
        assertNull(session.refreshAfter(PREVIOUS))
        assertNull(auth.stored)
        session.signIn()
        auth.remoteFails = true
        assertFailsWith<IllegalStateException> { session.signOut() }
        assertEquals(PREVIOUS, auth.revoked)
        assertNull(auth.stored)
    }

    @Test
    fun mapsConflictAndQuotaWithoutReplayingAndPreservesLongAndKey() = runTest {
        val auth = TestIdentity()
        var calls = 0
        val engine = MockEngine { req ->
            calls++
            when (calls) {
                1 -> {
                    assertEquals("stable-client-key", req.headers["Idempotency-Key"])
                    assertEquals("""{"expected_revision":9007199254740993}""", (req.body as TextContent).text)
                    respond("""{"error":"IDEMPOTENCY_CONFLICT","request_id":"conflict"}""", HttpStatusCode.Conflict)
                }
                2 -> respond("""{"error":"REVISION_CONFLICT"}""", HttpStatusCode.Conflict)
                3 -> respond("""{"error":"DAILY_LIMIT_REACHED"}""", HttpStatusCode.TooManyRequests)
                4 -> respond("""{"error":"PILOT_LIMIT_REACHED"}""", HttpStatusCode.TooManyRequests)
                else -> respond("""{"error":"REQUEST_RATE_LIMITED"}""", HttpStatusCode.TooManyRequests,
                    headersOf(HttpHeaders.RetryAfter, "22"))
            }
        }
        WhiteHatClient("https://api.example.test/api/v1", engine, NativeSession(auth, auth)).use { client ->
            val intent = RunSubmission(WORKSPACE, DECISION, 9_007_199_254_740_993L, "stable-client-key")
            assertEquals(Recovery.CHECK_EXISTING_RUN, assertFailsWith<ApiFailure> { client.enqueue(intent) }.recovery)
            assertEquals(Recovery.REFRESH_DECISION, assertFailsWith<ApiFailure> {
                client.replaceDecision(WORKSPACE, DECISION, 1, DecisionInput("Pilot", "Run a pilot?"))
            }.recovery)
            val quota = assertFailsWith<ApiFailure> { client.enqueue(intent) }
            assertEquals(Recovery.QUOTA_EXHAUSTED, quota.recovery)
            assertNull(quota.retryAfter)
            assertEquals(Recovery.QUOTA_EXHAUSTED, assertFailsWith<ApiFailure> { client.enqueue(intent) }.recovery)
            val rate = assertFailsWith<ApiFailure> { client.enqueue(intent) }
            assertEquals(Recovery.RATE_LIMITED, rate.recovery)
            assertEquals("22", rate.retryAfter)
        }
        assertEquals(5, calls)
        engine.close()
    }

    @Test
    fun stopsPollingOnCancellationAndReconciliationWithUnknownUsage() = runTest {
        val auth = TestIdentity()
        val states = listOf("queued", "running", "reconciliation_required")
        var calls = 0
        val engine = MockEngine {
            respond(runJson(states[calls++]))
        }
        WhiteHatClient("https://api.example.test/api/v1", engine, NativeSession(auth, auth)).use { client ->
            assertEquals("queued", client.poll(WORKSPACE, RUN).first().status)
            assertEquals(1, calls)
            val results = client.poll(WORKSPACE, RUN).toList()
            assertEquals(states.drop(1), results.map { it.status })
            assertNull(results.last().inputTokens)
            assertFalse(results.last().isActive)
        }
        assertEquals(3, calls)
        engine.close()
    }

    @Test
    fun rejectsNonTlsCredentialsAndRedirects() = runTest {
        val auth = TestIdentity()
        var calls = 0
        val engine = MockEngine {
            calls++
            respond("", HttpStatusCode.Found, headersOf(HttpHeaders.Location, "https://other.test/"))
        }
        val session = NativeSession(auth, auth)
        assertFailsWith<IllegalArgumentException> { WhiteHatClient("http://api.example.test/api/v1", engine, session) }
        assertFailsWith<IllegalArgumentException> { WhiteHatClient("https://user@api.example.test/api/v1", engine, session) }
        WhiteHatClient("https://api.example.test/api/v1", engine, session).use {
            assertEquals(302, assertFailsWith<ApiFailure> { it.me() }.status)
        }
        assertEquals(1, calls)
        engine.close()
    }

    private fun runJson(status: String) = """
        {"id":"$RUN","workspace_id":"$WORKSPACE","decision_id":"$DECISION","input_revision":1,
         "status":"$status","output_contract":"legacy-alpha.1","result":null,"error":null,
         "input_tokens":null,"output_tokens":null}
    """.trimIndent()
}
