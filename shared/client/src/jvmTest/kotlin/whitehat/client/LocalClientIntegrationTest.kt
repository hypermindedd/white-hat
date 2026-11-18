package whitehat.client

import io.ktor.client.engine.cio.CIO
import java.net.URI
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.test.runTest
import org.junit.Assume.assumeTrue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.seconds

class LocalClientIntegrationTest {
    @Test
    fun executesDecisionLifecycleAgainstIsolatedKtor() = runTest(timeout = 35.seconds) {
        val base = System.getenv("WH_C5_TEST_API")
        val bearer = System.getenv("WH_C5_TEST_TOKEN")
        assumeTrue("Requires an isolated fixture", !base.isNullOrBlank() && !bearer.isNullOrBlank())
        val issuer = System.getenv("WH_C5_TEST_ISSUER")
        val tokens = NativeTokens("c5-runtime", requireNotNull(bearer), null)
        val injected = object : NativeIdentity {
            override suspend fun signIn() = tokens
            override suspend fun refresh(current: NativeTokens): NativeTokens? = null
            override suspend fun revoke(current: NativeTokens) = Unit
        }
        val store = object : NativeTokenStore {
            private var stored: NativeTokens? = null
            override suspend fun read() = stored
            override suspend fun write(tokens: NativeTokens?) { stored = tokens }
        }
        val engine = CIO.create()
        val oidc = issuer?.let {
            val redirect = requireNotNull(System.getenv("WH_C5_TEST_REDIRECT"))
            val identityEngine = CIO.create()
            val identity = OidcNativeIdentity(
                NativeOidcConfig(issuer, "whitehat-web", "$issuer/authorize", "$issuer/token",
                    "$issuer/revoke", redirect),
                identityEngine,
                JvmLoopbackBrowser { url ->
                    val connection = url.toURL().openConnection() as HttpsURLConnection
                    connection.instanceFollowRedirects = false
                    connection.connectTimeout = 5_000
                    connection.readTimeout = 5_000
                    try {
                        assertEquals(302, connection.responseCode)
                        val callback = URI(requireNotNull(connection.getHeaderField("Location"))).toURL()
                        val wrongState = URI(callback.toString() + "&state=wrong").toURL()
                        assertEquals(400, (wrongState.openConnection() as java.net.HttpURLConnection).responseCode)
                        assertEquals(200, (callback.openConnection() as java.net.HttpURLConnection).responseCode)
                    } finally {
                        connection.disconnect()
                    }
                },
                JvmOidcProofs(), JvmRs256IdTokenVerifier(URI("$issuer/jwks")),
            )
            identity to identityEngine
        }
        try {
            WhiteHatClient(requireNotNull(base), engine, NativeSession(oidc?.first ?: injected, store), allowLocalHttp = true).use { client ->
                client.signIn()
                assertEquals(tokens.subject, client.me().subject)
                if (oidc != null) {
                    val current = requireNotNull(store.read())
                    val refreshed = requireNotNull(oidc.first.refresh(current))
                    assertNotNull(refreshed.refreshToken)
                    assertEquals(null, oidc.first.refresh(current))
                    store.write(refreshed)
                }
                assertEquals(7, client.modes().size)
                val workspace = client.createWorkspace("C5 client integration")
                assertEquals(emptyList(), client.decisions(workspace.id).items)
                val input = DecisionInput("Pilot", "Should we run a pilot?")
                val decision = client.createDecision(workspace.id, input)
                val edited = client.replaceDecision(workspace.id, decision.id, decision.revision, input.copy(title = "Revised pilot"))
                assertEquals(edited, client.decision(workspace.id, decision.id))
                val conflict = assertFailsWith<ApiFailure> {
                    client.replaceDecision(workspace.id, decision.id, decision.revision, input)
                }
                assertEquals(Recovery.REFRESH_DECISION, conflict.recovery)
                val intent = RunSubmission(workspace.id, decision.id, edited.revision, "test-stable-run")
                val submitted = client.enqueue(intent)
                assertEquals(submitted.id, client.enqueue(intent).id)
                val result = client.poll(workspace.id, submitted.id, 250).last()
                assertEquals("succeeded", result.status)
                assertNotNull(result.result)
                assertEquals(result.id, client.ownRuns(workspace.id).single().id)
                val beforeLogout = store.read()
                client.signOut()
                if (oidc != null) assertEquals(null, oidc.first.refresh(requireNotNull(beforeLogout)))
                assertEquals(Recovery.SIGN_IN, assertFailsWith<ApiFailure> { client.me() }.recovery)
            }
        } finally {
            oidc?.first?.close()
            oidc?.second?.close()
            engine.close()
        }
    }
}
