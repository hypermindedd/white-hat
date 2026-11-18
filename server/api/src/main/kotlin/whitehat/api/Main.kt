package whitehat.api

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.*
import java.util.UUID
import java.util.concurrent.TimeUnit
import whitehat.adapters.*
import whitehat.application.RunWorker

fun main(args: Array<String>) {
    require(args.isEmpty() || args.contentEquals(arrayOf("--migrate"))) { "Unsupported command" }
    fun required(name: String) = requireNotNull(System.getenv(name)?.takeIf { it.isNotBlank() }) { "Missing configuration: $name" }
    val databaseSettings = DatabaseRuntimeSettings.from(System.getenv())
    val config = HikariConfig().apply {
        jdbcUrl = required("WH_DATABASE_URL"); username = required("WH_DATABASE_USER"); password = required("WH_DATABASE_PASSWORD")
        maximumPoolSize = databaseSettings.poolSize; minimumIdle = 1
        connectionTimeout = databaseSettings.connectionTimeoutMillis.toLong(); validationTimeout = 250
        addDataSourceProperty("connectTimeout", 5)
        addDataSourceProperty("socketTimeout", (databaseSettings.statementTimeoutMillis + 999) / 1000 + 5)
    }
    HikariDataSource(config).use { dataSource ->
        if (args.contentEquals(arrayOf("--migrate"))) { PostgresStore.migrate(dataSource); return }
        PostgresStore.validate(dataSource)
        val verifier = accessIdentityVerifier(required("WH_AUTH_ISSUER"), required("WH_AUTH_JWKS_URL"), required("WH_AUTH_AUDIENCE"))
        val policy = EnginePolicy()
        val limit = System.getenv("WH_DAILY_RUN_LIMIT")?.toIntOrNull() ?: 20
        val store = PostgresStore(dataSource, policy.hash, limit, databaseSettings.statementTimeoutMillis, databaseSettings.lockTimeoutMillis)
        val voicePilot = if (System.getenv("WH_VOICE_PILOT") == "1") {
            val workspace = UUID.fromString(required("WH_VOICE_PILOT_WORKSPACE"))
            val provider = OpenAiLiveTranscription(required("WH_VOICE_PILOT_API_KEY"))
            VoicePilot(store, workspace, provider::open)
        } else null
        val g001 = if (System.getenv("WH_G001_PILOT") == "1") {
            val workspace = UUID.fromString(required("WH_G001_WORKSPACE"))
            val operator = required("WH_G001_OPERATOR_SUBJECT")
            require(required("WH_AI_MODEL") in setOf(G001Dispatch.BASELINE, G001Dispatch.CANDIDATE))
            required("WH_AI_API_KEY")
            G001Dispatch(workspace, operator, store::reserveG001Dispatch)
        } else null
        ResponsesProvider(System.getenv("WH_AI_API_KEY").orEmpty(), System.getenv("WH_AI_MODEL").orEmpty(), policy, g001 = g001).use { provider ->
            embeddedServer(Netty, host = System.getenv("WH_HOST") ?: "127.0.0.1", port = System.getenv("WH_PORT")?.toInt() ?: 8081) {
                whiteHatApi(store, verifier, voicePilot) { provider.configured }
                val worker = RunWorker(store, provider)
                val job = launch(Dispatchers.IO) {
                    while (isActive) {
                        try { if (!worker.tick()) delay(500) }
                        catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) { log.warn("RUN_WORKER_TICK_FAILED"); delay(1000) }
                    }
                }
                val retention = launch(Dispatchers.IO) {
                    while (isActive) {
                        try { store.sweepExpiredFeedback() }
                        catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) { log.warn("FEEDBACK_RETENTION_SWEEP_FAILED") }
                        delay(TimeUnit.HOURS.toMillis(1))
                    }
                }
                monitor.subscribe(ApplicationStopping) { job.cancel(); retention.cancel(); voicePilot?.close() }
            }.start(wait = true)
        }
    }
}
