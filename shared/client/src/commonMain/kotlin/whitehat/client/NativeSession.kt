package whitehat.client

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** The platform adapter supplies a validated OIDC subject and API access token. */
class NativeTokens(val subject: String, val accessToken: String, val refreshToken: String?) {
    init {
        require(subject.isNotBlank() && accessToken.isNotBlank())
    }
}

/** Login uses the system browser. The adapter owns PKCE, OIDC verification and token endpoint calls. */
interface NativeIdentity {
    suspend fun signIn(): NativeTokens

    /** Returns null when the session is no longer refreshable. Transient errors are thrown. */
    suspend fun refresh(current: NativeTokens): NativeTokens?

    suspend fun revoke(current: NativeTokens)
}

/** Back this with platform secure storage. A null value removes the local session. */
interface NativeTokenStore {
    suspend fun read(): NativeTokens?
    suspend fun write(tokens: NativeTokens?)
}

class NativeSession(private val identity: NativeIdentity, private val store: NativeTokenStore) {
    private val mutex = Mutex()

    suspend fun signIn() = mutex.withLock { store.write(identity.signIn()) }

    suspend fun signOut() = mutex.withLock {
        val previous = store.read()
        store.write(null)
        previous?.let { identity.revoke(it) }
    }

    internal suspend fun current(): NativeTokens? = mutex.withLock { store.read() }

    internal suspend fun refreshAfter(rejected: NativeTokens): NativeTokens? = mutex.withLock {
        val current = store.read() ?: return@withLock null
        if (current.subject != rejected.subject) return@withLock null
        if (current.accessToken != rejected.accessToken) return@withLock current
        val rotated = identity.refresh(current)?.takeIf { it.subject == current.subject }
        store.write(rotated)
        rotated
    }

    internal suspend fun discardRejected(rejected: NativeTokens) = mutex.withLock {
        val current = store.read()
        if (current?.subject == rejected.subject && current.accessToken == rejected.accessToken) {
            store.write(null)
        }
    }
}
