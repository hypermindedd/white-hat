# White Hat shared client

C5 client for the `/api/v1` resource API. The API contract is pinned to the
White Hat C3 handoff base `b07049ba11988b6b6ae816da40be48ab9a03d77f`.
See [the native session and recovery contract](../../docs/stages/S4-NATIVE-CLIENT-CONTRACT-FA.md).

Use a platform-owned Ktor engine, an implementation of `NativeIdentity`,
and a `NativeTokenStore` backed by that platform's secure storage. Pass an
absolute HTTPS URL whose path is `/api/v1`. Close both the client and its
caller-owned engine when the screen or application owner releases them.

```kotlin
val client = WhiteHatClient(
    "https://api.example.test/api/v1",
    platformEngine,
    NativeSession(platformIdentity, secureTokenStore),
)
client.signIn()
val profile = client.me()
```

Persist a `RunSubmission` before an enqueue that must survive a restart. On
an ambiguous response, keep the same workspace, decision, revision, and key.
After an accepted response, keep its run ID and resume polling that ID.
Stop collection when the UI no longer needs polling. Sign-out removes tokens
locally, even when provider revocation fails. The app can report such a failure
and must also clear any cached state or pending intents scoped to the account.

```sh
./gradlew :shared:client:jvmTest :shared:contracts:jvmTest \
  :shared:client:compileCommonMainKotlinMetadata
```

The JVM HTTP integration test runs with an isolated API fixture and fixture
token injected as `WH_C5_TEST_API` and `WH_C5_TEST_TOKEN`. Use a dedicated
synthetic workspace environment. Without the fixture, the integration test
is skipped. The contract applies equally to all four app targets and devices
still require client registration, secure storage, platform HTTP engines,
packaging and installation validation.

### Native OIDC wiring

`OidcNativeIdentity` implements authorization-code exchange, PKCE S256, refresh
and revocation. Supply approved HTTPS IdP endpoints, a public client ID,
an exact registered callback and a secure token store. An IdP refresh response
can omit the ID token. The original subject remains bound to that session.

Desktop JVM can use `JvmOidcProofs`, `JvmRs256IdTokenVerifier` and
`JvmLoopbackBrowser`. The browser adapter uses the configured loopback port.
The caller chooses and passes the per-session port where the IdP supports
arbitrary ports. Android can reuse the JVM proof and verification helpers and
supplies its own browser callback. iOS supplies platform proofs, signature
validation and callback. App hosts own OS credential storage on every platform.
