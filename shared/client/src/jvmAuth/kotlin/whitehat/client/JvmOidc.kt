package whitehat.client

import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.net.URI
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.interfaces.RSAPublicKey
import java.util.Base64
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class JvmOidcProofs : NativeOidcProofs {
    private val rng = SecureRandom()
    private val encoder = Base64.getUrlEncoder().withoutPadding()

    override fun randomUrlToken(): String = encoder.encodeToString(ByteArray(32).also(rng::nextBytes))

    override fun s256(verifier: String): String =
        encoder.encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)))
}

class JvmRs256IdTokenVerifier(jwksUri: URI) : NativeIdTokenVerifier {
    init {
        require(jwksUri.scheme == "https" && jwksUri.host != null && jwksUri.userInfo == null && jwksUri.fragment == null)
    }
    private val keys = JwkProviderBuilder(jwksUri.toURL()).cached(10, 1, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES).build()

    override suspend fun verify(token: String, issuer: String, audience: String, nonce: String?): String =
        withContext(Dispatchers.IO) {
            try {
                val decoded = JWT.decode(token)
                require(decoded.algorithm == "RS256")
                val key = keys.get(requireNotNull(decoded.keyId)).publicKey as RSAPublicKey
                val requirements = JWT.require(Algorithm.RSA256(key, null)).withIssuer(issuer)
                    .withAudience(audience).withClaimPresence("exp").withClaimPresence("iat")
                if (nonce != null) requirements.withClaim("nonce", nonce)
                val verified = requirements.build().verify(token)
                require(verified.type != "at+jwt" && verified.getClaim("token_use").asString() != "access")
                require(verified.audience.size == 1 || verified.getClaim("azp").asString() == audience)
                require(verified.getClaim("azp").isNull || verified.getClaim("azp").isMissing ||
                    verified.getClaim("azp").asString() == audience)
                requireNotNull(verified.subject).also { require(it.isNotBlank()) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                throw OidcFailure(null, "Identity token validation failed")
            }
        }
}
