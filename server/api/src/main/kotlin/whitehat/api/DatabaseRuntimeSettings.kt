package whitehat.api

/** Bounded deployment controls; invalid overrides fail startup instead of silently changing capacity. */
data class DatabaseRuntimeSettings(val poolSize: Int, val connectionTimeoutMillis: Int,
    val statementTimeoutMillis: Int, val lockTimeoutMillis: Int) {
    companion object {
        fun from(env: Map<String, String>): DatabaseRuntimeSettings {
            fun number(name: String, fallback: Int, range: IntRange): Int {
                val value = env[name]?.let { requireNotNull(it.toIntOrNull()) { "Invalid configuration: $name" } } ?: fallback
                require(value in range) { "Out-of-range configuration: $name" }
                return value
            }
            val statement = number("WH_DATABASE_STATEMENT_TIMEOUT_MS", 10000, 100..120000)
            return DatabaseRuntimeSettings(number("WH_DATABASE_POOL_SIZE", 8, 2..64),
                number("WH_DATABASE_CONNECTION_TIMEOUT_MS", 3000, 250..30000), statement,
                number("WH_DATABASE_LOCK_TIMEOUT_MS", 2000, 50..statement))
        }
    }
}
