package whitehat.api

import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.RSAKeyProvider
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.*
import java.net.URI
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
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
        val issuer = required("WH_AUTH_ISSUER"); val jwks = URI(required("WH_AUTH_JWKS_URL")); val audience = required("WH_AUTH_AUDIENCE")
        require(URI(issuer).scheme == "https" && jwks.scheme == "https" && jwks.userInfo == null) { "HTTPS identity configuration required" }
        val keys = JwkProviderBuilder(jwks.toURL()).cached(10, 6, TimeUnit.HOURS).rateLimited(10, 1, TimeUnit.MINUTES).build()
        val algorithm = Algorithm.RSA256(object : RSAKeyProvider {
            override fun getPublicKeyById(keyId: String?): RSAPublicKey = keys.get(requireNotNull(keyId)).publicKey as RSAPublicKey
            override fun getPrivateKey(): RSAPrivateKey? = null
            override fun getPrivateKeyId(): String? = null
        })
        val verifier = JWT.require(algorithm).withIssuer(issuer).withAudience(audience).withClaim("token_use", "access").build()
        val policy = EnginePolicy()
        val limit = System.getenv("WH_DAILY_RUN_LIMIT")?.toIntOrNull() ?: 20
        val store = PostgresStore(dataSource, policy.hash, limit, databaseSettings.statementTimeoutMillis, databaseSettings.lockTimeoutMillis)
        ResponsesProvider(System.getenv("WH_AI_API_KEY").orEmpty(), System.getenv("WH_AI_MODEL").orEmpty(), policy).use { provider ->
            embeddedServer(Netty, host = System.getenv("WH_HOST") ?: "127.0.0.1", port = System.getenv("WH_PORT")?.toInt() ?: 8081) {
                whiteHatApi(store, verifier) { provider.configured }
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
                monitor.subscribe(ApplicationStopping) { job.cancel(); retention.cancel() }
            }.start(wait = true)
        }
    }
}
