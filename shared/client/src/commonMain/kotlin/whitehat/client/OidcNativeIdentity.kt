package whitehat.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Values are obtained from the application's approved IdP registration. */
data class NativeOidcConfig(
    val issuer: String,
    val clientId: String,
    val authorizationEndpoint: String,
    val tokenEndpoint: String,
    val revocationEndpoint: String,
    val redirect: String,
    val scopes: List<String> = listOf("openid", "offline_access"),
) {
    init {
        require(clientId.isNotBlank() && "openid" in scopes)
        for (endpoint in listOf(issuer, authorizationEndpoint, tokenEndpoint, revocationEndpoint)) {
            val url = Url(endpoint)
            require(url.protocol == URLProtocol.HTTPS && url.host.isNotBlank() && url.user == null &&
                url.password == null && url.fragment.isEmpty())
        }
        val callback = Url(redirect)
        require(callback.host.isNotBlank() && callback.user == null && callback.password == null &&
            callback.encodedPath != "/" && callback.fragment.isEmpty() && callback.parameters.isEmpty())
        require(callback.protocol == URLProtocol.HTTPS ||
            (callback.protocol == URLProtocol.HTTP && callback.host in setOf("127.0.0.1", "::1", "[::1]")))
    }
}

/** The platform owns cryptographically secure randomness and SHA-256/base64url encoding. */
interface NativeOidcProofs {
    fun randomUrlToken(): String
    fun s256(verifier: String): String
}

/** Opens a system browser and returns the callback for this attempt. Cancels when the attempt closes. */
fun interface NativeAuthorizationBrowser {
    suspend fun authorize(url: String, redirect: String): String
}

/** Verifies signature against trusted JWKS, issuer, audience, expiry and the supplied nonce. */
fun interface NativeIdTokenVerifier {
    suspend fun verify(token: String, issuer: String, audience: String, nonce: String?): String
}

class OidcFailure(val status: Int?, message: String) : RuntimeException(message)

@Serializable
private data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("id_token") val idToken: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
)

@Serializable
private data class OAuthError(val error: String)

/** Authorization Code + PKCE implementation. Platform engines are caller-owned. */
class OidcNativeIdentity(
    private val config: NativeOidcConfig,
    engine: HttpClientEngine,
    private val browser: NativeAuthorizationBrowser,
    private val proofs: NativeOidcProofs,
    private val verifier: NativeIdTokenVerifier,
) : NativeIdentity, AutoCloseable {
    private val json = Json { ignoreUnknownKeys = true }
    private val http = HttpClient(engine) {
        followRedirects = false
        expectSuccess = false
        install(HttpTimeout) {
            connectTimeoutMillis = 10_000
            requestTimeoutMillis = 30_000
        }
    }

    override suspend fun signIn(): NativeTokens {
        val state = proofs.randomUrlToken()
        val nonce = proofs.randomUrlToken()
        val codeVerifier = proofs.randomUrlToken()
        require(setOf(state, nonce, codeVerifier).size == 3)
        require(listOf(state, nonce, codeVerifier).all { it.matches(Regex("[A-Za-z0-9_-]{43,128}")) })
        val challenge = proofs.s256(codeVerifier)
        require(challenge.matches(Regex("[A-Za-z0-9_-]{43}")))
        val authorization = URLBuilder(config.authorizationEndpoint).apply {
            parameters["response_type"] = "code"
            parameters["client_id"] = config.clientId
            parameters["redirect_uri"] = config.redirect
            parameters["scope"] = config.scopes.joinToString(" ")
            parameters["state"] = state
            parameters["nonce"] = nonce
            parameters["code_challenge"] = challenge
            parameters["code_challenge_method"] = "S256"
        }.buildString()
        val callback = Url(browser.authorize(authorization, config.redirect))
        val target = Url(config.redirect)
        if (callback.protocol != target.protocol || callback.host != target.host || callback.port != target.port ||
            callback.encodedPath != target.encodedPath || callback.fragment.isNotEmpty() ||
            callback.user != null || callback.password != null || callback.parameters.contains("error") ||
            callback.parameters.getAll("state")?.singleOrNull() != state ||
            (callback.parameters.contains("iss") && callback.parameters.getAll("iss")?.singleOrNull() != config.issuer)) {
            throw OidcFailure(null, "Invalid or rejected authorization callback")
        }
        val code = callback.parameters.getAll("code")?.singleOrNull()?.takeIf(String::isNotBlank)
            ?: throw OidcFailure(null, "Authorization code missing")
        val token = exchange(Parameters.build {
            append("grant_type", "authorization_code")
            append("client_id", config.clientId)
            append("redirect_uri", config.redirect)
            append("code", code)
            append("code_verifier", codeVerifier)
        }) ?: throw OidcFailure(null, "Authorization code rejected")
        val idToken = token.idToken ?: throw OidcFailure(null, "Identity token missing")
        val subject = verifier.verify(idToken, config.issuer, config.clientId, nonce)
        return NativeTokens(subject, token.accessToken, token.refreshToken)
    }

    override suspend fun refresh(current: NativeTokens): NativeTokens? {
        val refreshToken = current.refreshToken ?: return null
        val new = exchange(Parameters.build {
            append("grant_type", "refresh_token")
            append("client_id", config.clientId)
            append("refresh_token", refreshToken)
        }) ?: return null
        val subject = new.idToken?.let { verifier.verify(it, config.issuer, config.clientId, null) }
            ?: current.subject
        if (subject != current.subject) throw OidcFailure(null, "Identity changed during refresh")
        return NativeTokens(subject, new.accessToken, new.refreshToken ?: refreshToken)
    }

    override suspend fun revoke(current: NativeTokens) {
        val response = http.submitForm(config.revocationEndpoint, Parameters.build {
            append("client_id", config.clientId)
            append("token", current.refreshToken ?: current.accessToken)
            append("token_type_hint", if (current.refreshToken == null) "access_token" else "refresh_token")
        })
        response.bodyAsText()
        if (response.status.value !in 200..299) throw OidcFailure(response.status.value, "Token revocation failed")
    }

    private suspend fun exchange(form: Parameters): TokenResponse? {
        val response = http.submitForm(config.tokenEndpoint, form)
        val body = response.bodyAsText()
        if (response.status.value !in 200..299) {
            val code = runCatching { json.decodeFromString<OAuthError>(body).error }.getOrNull()
            if (response.status.value == 400 && code == "invalid_grant") return null
            throw OidcFailure(response.status.value, "Token exchange failed")
        }
        val tokens = runCatching { json.decodeFromString<TokenResponse>(body) }.getOrNull()
            ?: throw OidcFailure(response.status.value, "Invalid token response")
        if (!tokens.tokenType.equals("Bearer", ignoreCase = true) || tokens.accessToken.isBlank()) {
            throw OidcFailure(response.status.value, "Invalid bearer token response")
        }
        return tokens
    }

    override fun close() { http.close() }
}
