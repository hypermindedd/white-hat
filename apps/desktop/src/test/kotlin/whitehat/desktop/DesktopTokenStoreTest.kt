package whitehat.desktop

import java.nio.file.Files
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import whitehat.client.NativeTokens

class DesktopTokenStoreTest {
    @Test
    fun persistsRotatesAndRemovesSession() = runBlocking {
        val dir = Files.createTempDirectory("whitehat-credential-test")
        try {
            val path = dir.resolve("test.bin")
            // Reversible fixture for the file adapter. Native encryption is exercised separately.
            val transform = { bytes: ByteArray -> bytes.map { (it.toInt() xor 0x55).toByte() }.toByteArray() }
            val credential = ProtectedFileCredential(path, transform, transform)
            val store = DesktopTokenStore(credential)
            assertNull(store.read())

            store.write(NativeTokens("subject", "first access", "first refresh"))
            assertFalse(Files.readAllBytes(path).decodeToString().contains("first refresh"))
            val restored = DesktopTokenStore(ProtectedFileCredential(path, transform, transform))
            assertEquals("subject", restored.read()?.subject)
            assertEquals("first access", restored.read()?.accessToken)
            assertEquals("first refresh", restored.read()?.refreshToken)

            store.write(NativeTokens("subject", "rotated", null))
            assertEquals("rotated", restored.read()?.accessToken)
            assertNull(restored.read()?.refreshToken)
            restored.write(null)
            restored.write(null)
            assertNull(store.read())
            assertFalse(Files.exists(path))
        } finally {
            dir.toFile().deleteRecursively()
        }
        Unit
    }

    @Test
    fun failedProtectionLeavesPreviousCredentialReadable() {
        val dir = Files.createTempDirectory("whitehat-credential-test")
        try {
            val path = dir.resolve("test.bin")
            var fail = false
            val store = ProtectedFileCredential(path, { bytes ->
                check(!fail)
                bytes
            }, { it })
            store.write(byteArrayOf(1))
            fail = true
            assertFailsWith<IllegalStateException> { store.write(byteArrayOf(2)) }
            assertContentEquals(byteArrayOf(1), store.read())
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun scopesSessionsToIssuerClientAndApi() {
        val id = desktopCredentialId("https://issuer", "client", "https://api")
        assertEquals(id, desktopCredentialId("https://issuer", "client", "https://api"))
        assertNotEquals(id, desktopCredentialId("https://other", "client", "https://api"))
        assertNotEquals(id, desktopCredentialId("https://issuer", "other", "https://api"))
        assertNotEquals(id, desktopCredentialId("https://issuer", "client", "https://other"))
    }

    @Test
    fun nativeCredentialRoundTrip() = runBlocking {
        val os = System.getProperty("os.name").lowercase()
        val windows = os.startsWith("windows")
        val mac = os.startsWith("mac") && System.getenv("WH_TEST_MAC_KEYCHAIN") == "1"
        assumeTrue("Requires Windows DPAPI or an opted-in macOS Keychain", windows || mac)
        val id = desktopCredentialId("test", "test", UUID.randomUUID().toString())
        fun credential() = if (windows) windowsCredential(id) else MacCredential(id)

        val store = DesktopTokenStore(credential())
        try {
            assertNull(store.read())
            store.write(NativeTokens("fixture", "initial", "refresh"))
            val restored = DesktopTokenStore(credential())
            assertEquals("refresh", restored.read()?.refreshToken)
            restored.write(NativeTokens("fixture", "rotated", null))
            assertEquals("rotated", store.read()?.accessToken)
            assertNull(store.read()?.refreshToken)
            restored.write(null)
            assertNull(store.read())
        } finally {
            store.write(null)
        }
    }
}
