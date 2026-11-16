package whitehat.client

import io.ktor.client.engine.darwin.Darwin

/** iOS application owner supplies the ASWebAuthenticationSession, verifier and Keychain adapters. */
class IosClientConnection(
    apiBase: String,
    config: NativeOidcConfig,
    browser: NativeAuthorizationBrowser,
    proofs: NativeOidcProofs,
    verifier: NativeIdTokenVerifier,
    tokenStore: NativeTokenStore,
) : AutoCloseable {
    private val apiEngine = Darwin.create()
    private val identityEngine = Darwin.create()
    private val identity = OidcNativeIdentity(config, identityEngine, browser, proofs, verifier)
    val client = WhiteHatClient(apiBase, apiEngine, NativeSession(identity, tokenStore))

    override fun close() {
        client.close()
        identity.close()
        apiEngine.close()
        identityEngine.close()
    }
}
