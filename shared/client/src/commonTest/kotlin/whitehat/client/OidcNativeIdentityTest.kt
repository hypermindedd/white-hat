package whitehat.client

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.content.OutgoingContent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OidcNativeIdentityTest {
    private val config = NativeOidcConfig("https://issuer.test", "native-app", "https://issuer.test/authorize",
        "https://issuer.test/token", "https://issuer.test/revoke", "http://127.0.0.1:52345/oauth/callback")
    private val state = "a".repeat(43)
    private val nonce = "b".repeat(43)
    private val secretVerifier = "c".repeat(43)
    private val pkceChallenge = "d".repeat(43)
    private val proofs = object : NativeOidcProofs {
        private val tokens = listOf(state, nonce, secretVerifier).iterator()
        override fun randomUrlToken() = tokens.next()
        override fun s256(verifier: String): String {
            assertEquals(secretVerifier, verifier)
            return pkceChallenge
        }
    }
    private val browser = NativeAuthorizationBrowser { url, redirect ->
        val query = Url(url).parameters
        assertEquals(config.redirect, redirect)
        assertEquals(config.redirect, query["redirect_uri"])
        assertEquals("native-app", query["client_id"])
        assertEquals(state, query["state"])
        assertEquals(nonce, query["nonce"])
        assertEquals("S256", query["code_challenge_method"])
        assertEquals(pkceChallenge, query["code_challenge"])
        "$redirect?code=one-use-code&state=$state"
    }
    private val verify = NativeIdTokenVerifier { token, issuer, audience, receivedNonce ->
        assertEquals("signed-id-token", token)
        assertEquals(config.issuer, issuer)
        assertEquals(config.clientId, audience)
        assertEquals(nonce, receivedNonce)
        "synthetic-owner"
    }

    @Test
    fun signsInRotatesAndRevokesWithPublicClient() = runTest {
        var step = 0
        val engine = MockEngine { request ->
            val form = (request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()
            assertTrue("client_secret" !in form)
            when (++step) {
                1 -> {
                    assertEquals("/token", request.url.encodedPath)
                    assertTrue("grant_type=authorization_code" in form)
                    assertTrue("code_verifier=$secretVerifier" in form)
                    respond("""{"access_token":"initial","id_token":"signed-id-token","refresh_token":"r0","token_type":"Bearer"}""")
                }
                2 -> {
                    assertTrue("grant_type=refresh_token" in form && "refresh_token=r0" in form)
                    respond("""{"access_token":"fresh","refresh_token":"r1","token_type":"Bearer"}""")
                }
                else -> {
                    assertEquals("/revoke", request.url.encodedPath)
                    assertTrue("token=r1" in form && "token_type_hint=refresh_token" in form)
                    respond("")
                }
            }
        }
        OidcNativeIdentity(config, engine, browser, proofs, verify).use { identity ->
            val tokens = identity.signIn()
            assertEquals("synthetic-owner", tokens.subject)
            val rotated = requireNotNull(identity.refresh(tokens))
            assertEquals("fresh", rotated.accessToken)
            assertEquals("r1", rotated.refreshToken)
            identity.revoke(rotated)
        }
        assertEquals(3, step)
        engine.close()
    }

    @Test
    fun rejectsDuplicateStateBeforeTokenExchange() = runTest {
        val engine = MockEngine { error("Callback should fail before sending a request") }
        val malicious = NativeAuthorizationBrowser { _, redirect ->
            "$redirect?code=code&state=$state&state=$state"
        }
        OidcNativeIdentity(config, engine, malicious, proofs, verify).use {
            assertFailsWith<OidcFailure> { it.signIn() }
        }
        engine.close()
    }

    @Test
    fun rejectedRefreshEndsSessionAndTransientFailureSurfaces() = runTest {
        var calls = 0
        val engine = MockEngine {
            if (++calls == 1) respond("""{"error":"invalid_grant"}""", HttpStatusCode.BadRequest)
            else respond("", HttpStatusCode.ServiceUnavailable)
        }
        OidcNativeIdentity(config, engine, browser, proofs, verify).use {
            val tokens = NativeTokens("owner", "access", "refresh")
            assertNull(it.refresh(tokens))
            assertEquals(503, assertFailsWith<OidcFailure> { it.refresh(tokens) }.status)
        }
        engine.close()
    }
}
