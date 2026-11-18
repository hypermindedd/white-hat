package whitehat.api

import kotlin.test.*

class AccessIdentityTest {
    @Test fun deploymentAndReferenceRejectInsecureIdentityConfiguration() {
        for ((issuer, jwks, audience) in listOf(
            Triple("http://issuer.test", "https://issuer.test/keys", "api"),
            Triple("https://issuer.test", "http://issuer.test/keys", "api"),
            Triple("https://user:pass@issuer.test", "https://issuer.test/keys", "api"),
            Triple("https://issuer.test", "https://user:pass@issuer.test/keys", "api"),
            Triple("https://issuer.test", "https://issuer.test/keys", ""),
        )) assertFailsWith<IllegalArgumentException> { accessIdentityVerifier(issuer, jwks, audience) }
    }
}
