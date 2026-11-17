package whitehat.api

import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.JWTVerifier
import com.auth0.jwt.interfaces.RSAKeyProvider
import java.net.URI
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.concurrent.TimeUnit

/** The same resource-server verification is used by deployment and reference staging. */
fun accessIdentityVerifier(issuer: String, jwksUrl: String, audience: String): JWTVerifier {
    val issuerUri = URI(issuer); val jwks = URI(jwksUrl)
    require(issuerUri.scheme == "https" && issuerUri.host != null && issuerUri.userInfo == null &&
        jwks.scheme == "https" && jwks.host != null && jwks.userInfo == null && audience.isNotBlank()) {
        "HTTPS identity configuration required"
    }
    val keys = JwkProviderBuilder(jwks.toURL()).cached(10, 6, TimeUnit.HOURS).rateLimited(10, 1, TimeUnit.MINUTES).build()
    val algorithm = Algorithm.RSA256(object : RSAKeyProvider {
        override fun getPublicKeyById(keyId: String?): RSAPublicKey = keys.get(requireNotNull(keyId)).publicKey as RSAPublicKey
        override fun getPrivateKey(): RSAPrivateKey? = null
        override fun getPrivateKeyId(): String? = null
    })
    return JWT.require(algorithm).withIssuer(issuer).withAudience(audience).withClaim("token_use", "access").build()
}
