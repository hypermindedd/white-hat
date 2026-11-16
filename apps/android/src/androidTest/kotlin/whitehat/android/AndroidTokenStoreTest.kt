package whitehat.android

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import whitehat.client.NativeTokens

class AndroidTokenStoreTest {
    @Test
    fun encryptedSessionSurvivesStoreRecreationAndIsRemovedOnSignOut() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = AndroidTokenStore(context)
        val access = "synthetic-access-C5"
        val refresh = "synthetic-refresh-C5"
        store.write(null)
        try {
            store.write(NativeTokens("synthetic-user", access, refresh))
            val encrypted = context.getSharedPreferences("whitehat.session", 0).getString("encrypted", "")!!
            assertFalse(encrypted.contains(access))
            assertFalse(encrypted.contains(refresh))
            val restored = AndroidTokenStore(context).read()
            assertEquals("synthetic-user", restored?.subject)
            assertEquals(access, restored?.accessToken)
            assertEquals(refresh, restored?.refreshToken)
        } finally {
            store.write(null)
        }
        assertNull(AndroidTokenStore(context).read())
    }
}
