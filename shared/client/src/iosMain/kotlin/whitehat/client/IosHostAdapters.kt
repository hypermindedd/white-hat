package whitehat.client

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Callback contract for Swift platform services. A null value and error means no stored session. */
fun interface IosHostReply {
    fun complete(value: String?, error: String?)
}

interface IosHostPlatform {
    fun randomUrlToken(): String
    fun s256(verifier: String): String
    fun authorize(url: String, redirect: String, reply: IosHostReply)
    fun verify(token: String, issuer: String, audience: String, nonce: String?, reply: IosHostReply)
    fun readSession(): IosHostResult
    fun writeSession(value: String?): IosHostResult
}

data class IosHostResult(val value: String? = null, val error: String? = null)

@Serializable
private data class IosStoredSession(val subject: String, val accessToken: String, val refreshToken: String?)

private suspend fun platformCall(start: (IosHostReply) -> Unit): String =
    suspendCancellableCoroutine { continuation ->
        start(IosHostReply { value, error ->
            if (continuation.isActive) {
                if (error != null || value == null) continuation.resumeWithException(OidcFailure(null, error ?: "Platform request failed"))
                else continuation.resume(value)
            }
        })
    }

fun iosConnection(apiBase: String, config: NativeOidcConfig, platform: IosHostPlatform): IosClientConnection {
    val browser = NativeAuthorizationBrowser { url, redirect ->
        platformCall { platform.authorize(url, redirect, it) }
    }
    val verifier = NativeIdTokenVerifier { token, issuer, audience, nonce ->
        platformCall { platform.verify(token, issuer, audience, nonce, it) }
    }
    val proofs = object : NativeOidcProofs {
        override fun randomUrlToken() = platform.randomUrlToken()
        override fun s256(verifier: String) = platform.s256(verifier)
    }
    val store = object : NativeTokenStore {
        override suspend fun read(): NativeTokens? {
            val result = platform.readSession()
            result.error?.let { throw OidcFailure(null, it) }
            val data = result.value?.let { Json.decodeFromString<IosStoredSession>(it) } ?: return null
            return NativeTokens(data.subject, data.accessToken, data.refreshToken)
        }

        override suspend fun write(tokens: NativeTokens?) {
            val data = tokens?.let { Json.encodeToString(IosStoredSession(it.subject, it.accessToken, it.refreshToken)) }
            platform.writeSession(data).error?.let { throw OidcFailure(null, it) }
        }
    }
    return IosClientConnection(apiBase, config, browser, proofs, verifier, store)
}
