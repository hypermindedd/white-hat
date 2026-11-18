package whitehat.api

import kotlin.test.*

class DatabaseRuntimeSettingsTest {
    @Test fun boundedDefaultsPreserveBaselineCapacity() {
        assertEquals(DatabaseRuntimeSettings(8, 3000, 10000, 2000), DatabaseRuntimeSettings.from(emptyMap()))
    }
    @Test fun configurationRejectsUnboundedOrMalformedValues() {
        for ((name, value) in listOf("WH_DATABASE_POOL_SIZE" to "10000", "WH_DATABASE_CONNECTION_TIMEOUT_MS" to "0",
            "WH_DATABASE_STATEMENT_TIMEOUT_MS" to "abc", "WH_DATABASE_LOCK_TIMEOUT_MS" to "10001")) {
            assertFailsWith<IllegalArgumentException> { DatabaseRuntimeSettings.from(mapOf(name to value)) }
        }
    }
    @Test fun deliberateLowLatencyProfileMustKeepLockWithinStatementBudget() {
        val configured = DatabaseRuntimeSettings.from(mapOf("WH_DATABASE_POOL_SIZE" to "16",
            "WH_DATABASE_STATEMENT_TIMEOUT_MS" to "1000", "WH_DATABASE_LOCK_TIMEOUT_MS" to "100"))
        assertEquals(16, configured.poolSize); assertEquals(100, configured.lockTimeoutMillis)
    }
}
