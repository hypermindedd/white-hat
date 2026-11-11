package whitehat.api

import com.auth0.jwt.interfaces.JWTVerifier
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.*
import java.io.ByteArrayOutputStream
import java.util.UUID
import whitehat.contracts.Mode
import whitehat.domain.*

private suspend fun <T> db(block: () -> T): T = withContext(Dispatchers.IO) { block() }
private suspend fun ApplicationCall.json(value: JsonElement, status: HttpStatusCode = HttpStatusCode.OK) = respondText(value.toString(), ContentType.Application.Json, status)
private suspend fun ApplicationCall.failure(status: Int, code: String) = json(buildJsonObject {
    put("error", code); put("request_id", callId ?: UUID.randomUUID().toString())
}, HttpStatusCode.fromValue(status))
private fun ApplicationCall.subject(): String = principal<JWTPrincipal>()!!.payload.subject
private fun ApplicationCall.id(name: String): UUID = try { UUID.fromString(parameters[name]) } catch (_: Exception) { throw Fault(400, "INVALID_ID") }
private fun JsonObject.checkKeys(allowed: Set<String>) { if (keys.any { it !in allowed }) throw Fault(400, "UNEXPECTED_FIELD") }
private fun JsonObject.string(key: String, fallback: String? = null): String {
    val value = get(key) ?: return fallback ?: throw Fault(400, "INVALID_INPUT")
    return (value as? JsonPrimitive)?.takeIf { it.isString }?.content ?: throw Fault(400, "INVALID_INPUT")
}
private fun JsonObject.revision(): Long = (get("expected_revision") as? JsonPrimitive)?.takeUnless { it.isString }?.longOrNull?.takeIf { it > 0 } ?: throw Fault(400, "EXPECTED_REVISION_REQUIRED")
private val decisionKeys = setOf("title", "decision", "objective", "context", "source_notes", "mode")
private fun JsonObject.input(): DecisionInput = DecisionInput(string("title"), string("decision"), string("objective", ""), string("context", ""), string("source_notes", ""),
    Mode.fromWire(string("mode", "challenge")) ?: throw Fault(400, "INVALID_MODE")).also { it.validate() }

private suspend fun ApplicationCall.objectBody(): JsonObject {
    if (!request.contentType().match(ContentType.Application.Json)) throw Fault(415, "JSON_REQUIRED")
    val maxBytes = 140000
    if ((request.headers[HttpHeaders.ContentLength]?.toLongOrNull() ?: 0) > maxBytes) throw Fault(413, "INPUT_TOO_LARGE")
    val channel = receiveChannel(); val output = ByteArrayOutputStream(); val buffer = ByteArray(8192)
    while (true) {
        val count = channel.readAvailable(buffer, 0, minOf(buffer.size, maxBytes + 1 - output.size()))
        if (count == -1) break
        if (count == 0) { yield(); continue }
        output.write(buffer, 0, count)
        if (output.size() > maxBytes) throw Fault(413, "INPUT_TOO_LARGE")
    }
    return try { Json.parseToJsonElement(output.toString(Charsets.UTF_8)).jsonObject } catch (_: Exception) { throw Fault(400, "INVALID_JSON") }
}

private fun Workspace.json() = buildJsonObject { put("id", id.toString()); put("name", name); put("role", role.name) }
private fun Decision.json() = buildJsonObject {
    put("id", id.toString()); put("workspace_id", workspace.toString()); put("revision", revision)
    put("title", input.title); put("decision", input.decision); put("objective", input.objective)
    put("context", input.context); put("source_notes", input.sourceNotes); put("mode", input.mode.wireId)
}
private fun Run.json() = buildJsonObject {
    put("id", id.toString()); put("workspace_id", workspace.toString()); put("decision_id", decision.toString())
    put("input_revision", revision); put("status", status); put("output_contract", "legacy-alpha.1")
    put("result", result?.let(Json::parseToJsonElement) ?: JsonNull); put("error", error?.let(::JsonPrimitive) ?: JsonNull)
    put("input_tokens", inputTokens?.let(::JsonPrimitive) ?: JsonNull); put("output_tokens", outputTokens?.let(::JsonPrimitive) ?: JsonNull)
}

fun Application.whiteHatApi(store: DecisionStore, tokenVerifier: JWTVerifier, providerConfigured: () -> Boolean) {
    install(CallId) { generate { UUID.randomUUID().toString() }; replyToHeader("X-Request-ID") }
    install(DefaultHeaders) {
        header("Cache-Control", "no-store"); header("X-Content-Type-Options", "nosniff"); header("Referrer-Policy", "no-referrer")
    }
    install(StatusPages) {
        exception<Fault> { call, cause -> call.failure(cause.status, cause.code) }
        exception<Throwable> { call, cause ->
            if (cause is CancellationException) throw cause
            // No provider bodies, token values, transcript or SQL are exposed.
            call.failure(500, "INTERNAL_ERROR")
        }
    }
    install(Authentication) {
        jwt("access") {
            verifier(tokenVerifier)
            validate { credential ->
                val payload = credential.payload
                if (!payload.subject.isNullOrBlank() && payload.subject.length <= 200 && payload.expiresAt != null && payload.getClaim("token_use").asString() == "access") JWTPrincipal(payload) else null
            }
            challenge { _, _ -> call.failure(401, "AUTH_REQUIRED") }
        }
    }
    routing {
        get("/api/v1/health") { call.json(buildJsonObject { put("status", "ok"); put("version", "0.4.0-alpha.1") }) }
        get("/api/v1/openapi.json") {
            call.respondText(requireNotNull(javaClass.getResourceAsStream("/openapi.json")).use { it.readBytes().toString(Charsets.UTF_8) }, ContentType.Application.Json)
        }
        authenticate("access") {
            get("/api/v1/me") {
                val actor = call.subject(); val workspaces = db { store.workspaces(actor) }
                call.json(buildJsonObject { put("subject", actor); put("workspaces", JsonArray(workspaces.map { it.json() })) })
            }
            get("/api/v1/modes") {
                call.json(buildJsonObject { put("items", JsonArray(Mode.entries.map { mode -> buildJsonObject {
                    put("id", mode.wireId); put("verdicts", JsonArray((mode.verdicts + "NEEDS_CONTEXT").map(::JsonPrimitive)))
                } })) })
            }
            post("/api/v1/workspaces") {
                val body = call.objectBody(); body.checkKeys(setOf("name")); val actor = call.subject()
                val workspace = db { store.createWorkspace(actor, body.string("name")) }
                call.json(workspace.json(), HttpStatusCode.Created)
            }
            post("/api/v1/workspaces/{workspace}/members") {
                val body = call.objectBody(); body.checkKeys(setOf("subject", "role", "active"))
                val active = (body["active"] as? JsonPrimitive)?.takeUnless { it.isString }?.booleanOrNull ?: throw Fault(400, "INVALID_INPUT")
                val role = runCatching { Role.valueOf(body.string("role")) }.getOrElse { throw Fault(400, "INVALID_ROLE") }
                val actor = call.subject(); val workspace = call.id("workspace")
                db { store.setMember(actor, workspace, body.string("subject"), role, active) }
                call.json(buildJsonObject { put("ok", true) })
            }
            route("/api/v1/workspaces/{workspace}/decisions") {
                get {
                    val actor = call.subject(); val workspace = call.id("workspace")
                    val decisions = db { store.decisions(actor, workspace) }
                    call.json(buildJsonObject { put("items", JsonArray(decisions.map { it.json() })); put("limit", 100) })
                }
                post {
                    val body = call.objectBody(); body.checkKeys(decisionKeys); val input = body.input()
                    val actor = call.subject(); val workspace = call.id("workspace")
                    call.json(db { store.createDecision(actor, workspace, input) }.json(), HttpStatusCode.Created)
                }
                get("/{decision}") {
                    val actor = call.subject(); val workspace = call.id("workspace"); val id = call.id("decision")
                    call.json(db { store.decision(actor, workspace, id) }.json())
                }
                put("/{decision}") {
                    val body = call.objectBody(); body.checkKeys(decisionKeys + "expected_revision")
                    val revision = body.revision(); val input = body.input()
                    val actor = call.subject(); val workspace = call.id("workspace"); val id = call.id("decision")
                    call.json(db { store.editDecision(actor, workspace, id, revision, input) }.json())
                }
                post("/{decision}/runs") {
                    val body = call.objectBody(); body.checkKeys(setOf("expected_revision")); val revision = body.revision()
                    val key = call.request.headers["Idempotency-Key"] ?: throw Fault(400, "IDEMPOTENCY_KEY_REQUIRED")
                    val actor = call.subject(); val workspace = call.id("workspace"); val id = call.id("decision")
                    // Authorize before revealing provider availability.
                    db { store.decision(actor, workspace, id) }
                    if (!providerConfigured()) throw Fault(409, "PROVIDER_NOT_CONFIGURED")
                    call.json(db { store.enqueue(actor, workspace, id, revision, key) }.json(), HttpStatusCode.Accepted)
                }
            }
            get("/api/v1/workspaces/{workspace}/runs/{run}") {
                val actor = call.subject(); val workspace = call.id("workspace"); val id = call.id("run")
                call.json(db { store.run(actor, workspace, id) }.json())
            }
            post("/api/v1/workspaces/{workspace}/runs/{run}/cancel") {
                val actor = call.subject(); val workspace = call.id("workspace"); val id = call.id("run")
                call.json(db { store.cancel(actor, workspace, id) }.json())
            }
            get("/api/v1/workspaces/{workspace}/admin/usage") {
                val actor = call.subject(); val workspace = call.id("workspace")
                val usage = db { store.adminUsage(actor, workspace) }
                call.json(buildJsonObject { usage.forEach { (key, value) -> put(key, value?.let(::JsonPrimitive) ?: JsonNull) } })
            }
        }
    }
}
