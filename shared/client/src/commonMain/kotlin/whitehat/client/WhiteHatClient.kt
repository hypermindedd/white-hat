package whitehat.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.encodeURLPathPart
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement

enum class Recovery {
    SIGN_IN, RETRY_AFTER_REFRESH, REFRESH_DECISION, CHECK_EXISTING_RUN, QUOTA_EXHAUSTED, RATE_LIMITED,
    ANALYSIS_PAUSED, PROVIDER_UNAVAILABLE, MANUAL,
}

class ApiFailure(
    val status: Int,
    val code: String,
    val requestId: String?,
    val retryAfter: String?,
    val recovery: Recovery,
) : RuntimeException("White Hat API: HTTP $status ($code)")

@Serializable
private data class ErrorResponse(val error: String, @SerialName("request_id") val requestId: String? = null)

@Serializable
private data class Items<T>(val items: List<T>)

class WhiteHatClient(
    apiBase: String,
    engine: HttpClientEngine,
    private val session: NativeSession,
    allowLocalHttp: Boolean = false,
) : AutoCloseable {
    private val base = apiBase.trimEnd('/')
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val http: HttpClient

    init {
        val url = Url(base)
        require(url.host.isNotBlank() && url.user == null && url.password == null)
        require(url.encodedPath == "/api/v1" && url.parameters.isEmpty() && url.fragment.isEmpty())
        require(url.protocol == URLProtocol.HTTPS || (allowLocalHttp && url.protocol == URLProtocol.HTTP &&
            url.host in setOf("127.0.0.1", "localhost", "::1", "[::1]")))
        http = HttpClient(engine) {
            followRedirects = false
            expectSuccess = false
            install(HttpTimeout) {
                connectTimeoutMillis = 10_000
                requestTimeoutMillis = 30_000
            }
        }
    }

    suspend fun signIn() { session.signIn() }
    suspend fun signOut() { session.signOut() }
    suspend fun me(): Profile = request(HttpMethod.Get, "/me")
    suspend fun modes(): List<DecisionMode> = request<Items<DecisionMode>>(HttpMethod.Get, "/modes").items

    suspend fun createWorkspace(name: String): Workspace =
        request(HttpMethod.Post, "/workspaces", JsonObject(mapOf("name" to JsonPrimitive(name))))

    suspend fun decisions(workspace: String): Decisions = request(HttpMethod.Get, decisionsPath(workspace))

    suspend fun createDecision(workspace: String, input: DecisionInput): Decision =
        request(HttpMethod.Post, decisionsPath(workspace), json.encodeToJsonElement(input) as JsonObject)

    suspend fun decision(workspace: String, decision: String): Decision =
        request(HttpMethod.Get, decisionPath(workspace, decision))

    suspend fun replaceDecision(workspace: String, decision: String, revision: Long, input: DecisionInput): Decision {
        require(revision > 0)
        val fields = json.encodeToJsonElement(input) as JsonObject
        return request(HttpMethod.Put, decisionPath(workspace, decision),
            JsonObject(fields + ("expected_revision" to JsonPrimitive(revision))))
    }

    suspend fun enqueue(submission: RunSubmission): Run = request(
        HttpMethod.Post, decisionPath(submission.workspace, submission.decision) + "/runs",
        JsonObject(mapOf("expected_revision" to JsonPrimitive(submission.revision))), submission.idempotencyKey,
    )

    suspend fun ownRuns(workspace: String): List<Run> =
        request<Items<Run>>(HttpMethod.Get, "${workspacePath(workspace)}/runs").items

    suspend fun run(workspace: String, run: String): Run =
        request(HttpMethod.Get, runPath(workspace, run))

    suspend fun cancel(workspace: String, run: String): Run =
        request(HttpMethod.Post, runPath(workspace, run) + "/cancel")

    fun poll(workspace: String, runId: String, intervalMillis: Long = 1_000) = flow {
        require(intervalMillis >= 250)
        do {
            val current = run(workspace, runId)
            emit(current)
            if (current.isActive) delay(intervalMillis)
        } while (current.isActive)
    }

    override fun close() { http.close() }

    private fun segment(id: String): String {
        require(id.matches(Regex("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}")))
        return id.encodeURLPathPart()
    }

    private fun workspacePath(workspace: String) = "/workspaces/${segment(workspace)}"
    private fun decisionsPath(workspace: String) = workspacePath(workspace) + "/decisions"
    private fun decisionPath(workspace: String, decision: String) = decisionsPath(workspace) + "/" + segment(decision)
    private fun runPath(workspace: String, run: String) = workspacePath(workspace) + "/runs/" + segment(run)

    private suspend inline fun <reified T> request(
        method: HttpMethod, path: String, body: JsonObject? = null, key: String? = null,
    ): T {
        val token = session.current() ?: throw ApiFailure(401, "AUTH_REQUIRED", null, null, Recovery.SIGN_IN)
        var response = send(method, path, token, body, key)
        if (response.status.value == 401) {
            // Consume the body before another request. Mutations return the original error.
            val failure = failure(response, response.bodyAsText())
            val fresh = session.refreshAfter(token) ?: throw failure
            if (method != HttpMethod.Get) throw ApiFailure(401, failure.code, failure.requestId,
                failure.retryAfter, Recovery.RETRY_AFTER_REFRESH)
            response = send(method, path, fresh, body, key)
            if (response.status.value == 401) session.discardRejected(fresh)
        }
        val content = response.bodyAsText()
        if (response.status.value !in 200..299) throw failure(response, content)
        return json.decodeFromString<T>(content)
    }

    private suspend fun send(method: HttpMethod, path: String, token: NativeTokens, body: JsonObject?, key: String?) =
        http.request(base + path) {
            this.method = method
            bearerAuth(token.accessToken)
            if (key != null) headers.append("Idempotency-Key", key)
            if (body != null) {
                contentType(ContentType.Application.Json)
                setBody(body.toString())
            }
        }

    private fun failure(response: HttpResponse, content: String): ApiFailure {
        val status = response.status.value
        val error = runCatching { json.decodeFromString<ErrorResponse>(content) }.getOrNull()
        val code = error?.error ?: "HTTP_$status"
        val recovery = when {
            status == 401 -> Recovery.SIGN_IN
            status == 409 && code == "REVISION_CONFLICT" -> Recovery.REFRESH_DECISION
            status == 409 && code in setOf("IDEMPOTENCY_CONFLICT", "ACTIVE_RUN_EXISTS", "RUN_ALREADY_TERMINAL") -> Recovery.CHECK_EXISTING_RUN
            status == 409 && code == "ANALYSIS_PAUSED" -> Recovery.ANALYSIS_PAUSED
            status == 409 && code == "PROVIDER_NOT_CONFIGURED" -> Recovery.PROVIDER_UNAVAILABLE
            status == 429 && (code == "DAILY_LIMIT_REACHED" || code == "PILOT_LIMIT_REACHED") -> Recovery.QUOTA_EXHAUSTED
            status == 429 -> Recovery.RATE_LIMITED
            else -> Recovery.MANUAL
        }
        return ApiFailure(status, code, error?.requestId ?: response.headers["X-Request-ID"],
            response.headers[HttpHeaders.RetryAfter], recovery)
    }
}
