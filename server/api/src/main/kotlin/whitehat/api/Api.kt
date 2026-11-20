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

private fun planPreview(estimates: List<PlanEstimate>) = buildJsonObject {
    put("catalog_version", CommercialPlans.version); put("status", "proposal"); put("billing_enabled", false)
    putJsonArray("items") { estimates.forEach { estimate -> add(buildJsonObject {
        put("id", estimate.plan.id)
        put("seats", estimate.plan.seats?.let(::JsonPrimitive) ?: JsonNull)
        put("monthly_attempts", estimate.plan.monthlyAttempts?.let(::JsonPrimitive) ?: JsonNull)
        put("estimated_cost_usd_micros", estimate.estimatedCostMicros?.let(::JsonPrimitive) ?: JsonNull)
        put("revenue_floor_usd_micros", estimate.revenueFloorMicros?.let(::JsonPrimitive) ?: JsonNull)
    }) } }
}
private fun JsonObject.costAssumption(name: String): Long = (get(name) as? JsonPrimitive)
    ?.takeUnless { it.isString }?.longOrNull?.takeIf { it >= 0 } ?: throw Fault(400, "INVALID_PLAN_ASSUMPTION")

internal fun Application.whiteHatApi(store: DecisionStore, tokenVerifier: JWTVerifier, voicePilot: VoicePilot? = null, providerConfigured: () -> Boolean) {
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
        get("/api/v1/ready") {
            db { store.checkReady() }
            call.json(buildJsonObject { put("status", "ready") })
        }
        get("/api/v1/openapi.json") {
            call.respondText(requireNotNull(javaClass.getResourceAsStream("/openapi.json")).use { it.readBytes().toString(Charsets.UTF_8) }, ContentType.Application.Json)
        }
        authenticate("access") {
            if (voicePilot != null) route("/api/v1/workspaces/{workspace}/voice-pilot") {
                get {
                    db { voicePilot.check(call.subject(), call.id("workspace")) }
                    call.json(buildJsonObject { put("max_seconds", 15) })
                }
                post {
                    val body = call.objectBody()
                    body.checkKeys(emptySet())
                    val lease = db { voicePilot.start(call.subject(), call.id("workspace")) }
                    call.json(buildJsonObject { put("lease_id", lease.toString()) }, HttpStatusCode.Created)
                }
                post("/{lease}/chunk") {
                    val body = call.objectBody()
                    body.checkKeys(setOf("pcm"))
                    db { voicePilot.append(call.subject(), call.id("workspace"), call.id("lease"), body.string("pcm")) }
                    call.json(buildJsonObject { put("accepted", true) })
                }
                post("/{lease}/finish") {
                    call.objectBody().checkKeys(emptySet())
                    val transcript = db { voicePilot.finish(call.subject(), call.id("workspace"), call.id("lease")) }
                    call.json(buildJsonObject { put("transcript", transcript) })
                }
                post("/{lease}/decision") {
                    call.objectBody().checkKeys(emptySet())
                    val decision = db { voicePilot.confirm(call.subject(), call.id("workspace"), call.id("lease")) }
                    call.json(decision.json())
                }
                post("/{lease}/cancel") {
                    call.objectBody().checkKeys(emptySet())
                    db { voicePilot.cancel(call.subject(), call.id("workspace"), call.id("lease")) }
                    call.json(buildJsonObject { put("cancelled", true) })
                }
            }
            get("/api/v1/workspaces/{workspace}/admin/plan-preview") {
                val actor = call.subject(); val workspace = call.id("workspace")
                db { store.requireAdministrator(actor, workspace) }
                call.json(planPreview(CommercialPlans.proposals.map { PlanEstimate(it, null, null) }))
            }
            post("/api/v1/workspaces/{workspace}/admin/plan-preview") {
                val actor = call.subject(); val workspace = call.id("workspace")
                db { store.requireAdministrator(actor, workspace) }
                val body = call.objectBody()
                body.checkKeys(setOf("assumed_cost_per_run_usd_micros", "fixed_monthly_cost_usd_micros", "target_margin_bps"))
                val margin = (body["target_margin_bps"] as? JsonPrimitive)?.takeUnless { it.isString }?.intOrNull
                    ?: throw Fault(400, "INVALID_PLAN_ASSUMPTION")
                call.json(planPreview(CommercialPlans.estimate(body.costAssumption("assumed_cost_per_run_usd_micros"),
                    body.costAssumption("fixed_monthly_cost_usd_micros"), margin)))
            }
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
            get("/api/v1/workspaces/{workspace}/members") {
                val actor = call.subject(); val workspace = call.id("workspace")
                val items = db { store.members(actor, workspace) }
                call.json(buildJsonObject { putJsonArray("items") { items.forEach { member ->
                    add(buildJsonObject {
                        put("subject", member.subject); put("role", member.role.name)
                        put("active", member.active); put("can_review_incidents", member.canReviewIncidents)
                        putJsonArray("permissions") { member.permissions.forEach { add(it.name) } }
                        put("support_role_id", member.supportRoleId?.toString())
                    })
                } } })
            }
            put("/api/v1/workspaces/{workspace}/operator-permissions") {
                val body = call.objectBody(); body.checkKeys(setOf("subject", "enabled", "permission"))
                val enabled = (body["enabled"] as? JsonPrimitive)?.takeUnless { it.isString }?.booleanOrNull
                    ?: throw Fault(400, "INVALID_INPUT")
                val subject = body.string("subject")
                if(subject.isBlank() || subject.length > 200) throw Fault(400, "INVALID_INPUT")
                val actor = call.subject(); val workspace = call.id("workspace")
                val permission = runCatching { OperatorPermission.valueOf(body.string("permission", "REVIEW_RUN_INCIDENTS")) }
                    .getOrElse { throw Fault(400, "INVALID_PERMISSION") }
                db { store.setOperatorPermission(actor, workspace, subject, permission, enabled) }
                call.json(buildJsonObject { put("ok", true) })
            }
            get("/api/v1/workspaces/{workspace}/runs") {
                val actor = call.subject(); val workspace = call.id("workspace")
                val items = db { store.runHistory(actor, workspace) }
                call.json(buildJsonObject { put("items", JsonArray(items.map { it.json() })) })
            }
            get("/api/v1/workspaces/{workspace}/incidents") {
                val actor = call.subject(); val workspace = call.id("workspace")
                val items = db { store.incidents(actor, workspace) }
                call.json(buildJsonObject { putJsonArray("items") { items.forEach { incident ->
                    add(buildJsonObject {
                        put("run_id", incident.runId.toString()); put("disposition", incident.disposition?.name)
                    })
                } } })
            }
            put("/api/v1/workspaces/{workspace}/incidents/{run}") {
                val body = call.objectBody(); body.checkKeys(setOf("disposition"))
                val disposition = runCatching { IncidentDisposition.valueOf(body.string("disposition")) }
                    .getOrElse { throw Fault(400, "INVALID_DISPOSITION") }
                val actor = call.subject(); val workspace = call.id("workspace"); val runId = call.id("run")
                db { store.reviewIncident(actor, workspace, runId, disposition) }
                call.json(buildJsonObject { put("ok", true) })
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
            route("/api/v1/workspaces/{workspace}") {
                get("/support-roles") {
                    val actor = call.subject(); val workspace = call.id("workspace")
                    val roles = db { store.supportRoles(actor, workspace) }
                    call.json(buildJsonObject { putJsonArray("items") { roles.forEach { role ->
                        add(buildJsonObject {
                            put("id", role.id.toString()); put("name", role.name)
                            putJsonArray("permissions") { role.permissions.forEach { add(it.name) } }
                        })
                    } } })
                }
                put("/support-roles/{supportRole}") {
                    val body = call.objectBody(); body.checkKeys(setOf("name", "permissions"))
                    val items = body["permissions"] as? JsonArray ?: throw Fault(400, "INVALID_PERMISSION")
                    val permissions = items.map {
                        val name = (it as? JsonPrimitive)?.takeIf { value -> value.isString }?.content
                        OperatorPermission.entries.find { permission -> permission.name == name }
                            ?: throw Fault(400, "INVALID_PERMISSION")
                    }.toSet()
                    if (items.size != permissions.size) throw Fault(400, "INVALID_PERMISSION")
                    val role = SupportRole(call.id("supportRole"), body.string("name"), permissions)
                    val actor = call.subject(); val workspace = call.id("workspace")
                    db { store.saveSupportRole(actor, workspace, role) }
                    call.json(buildJsonObject { put("ok", true) })
                }
                put("/support-role-assignment") {
                    val body = call.objectBody(); body.checkKeys(setOf("subject", "role_id"))
                    val subject = body.string("subject")
                    if (subject.isBlank() || subject.length > 200) throw Fault(400, "INVALID_INPUT")
                    val role = if (body["role_id"] == JsonNull) null
                        else runCatching { UUID.fromString(body.string("role_id")) }
                            .getOrElse { throw Fault(400, "INVALID_ID") }
                    val actor = call.subject(); val workspace = call.id("workspace")
                    db { store.assignSupportRole(actor, workspace, subject, role) }
                    call.json(buildJsonObject { put("ok", true) })
                }
                get("/operations-metrics") {
                    val actor = call.subject(); val workspace = call.id("workspace")
                    val values = db { store.operationsMetrics(actor, workspace) }
                    call.json(buildJsonObject { values.forEach { (key, value) -> put(key, value) } })
                }
                get("/admin/pilot-usage") {
                    val actor = call.subject(); val workspace = call.id("workspace")
                    val report = db { store.pilotUsage(actor, workspace) }
                    call.json(buildJsonObject {
                        put("period_start_utc", report.periodStart.toString())
                        put("entitlement_revision", report.entitlementRevision)
                        put("monthly_attempt_limit", report.attemptLimit)
                        put("metered_attempts", report.meteredAttempts)
                        put("pilot_runs", report.pilotRuns)
                        put("reconciled", report.reconciled)
                        put("confirmed_cost_runs", report.confirmedCostRuns)
                        put("unconfirmed_cost_runs", report.pilotRuns - report.confirmedCostRuns)
                        put("confirmed_usd_micros", report.confirmedUsdMicros)
                    })
                }
                get("/admin/costs") {
                    val actor = call.subject(); val workspace = call.id("workspace")
                    val values = db { store.confirmedCosts(actor, workspace) }
                    call.json(buildJsonObject { values.forEach { (key, value) -> put(key, value) } })
                }
                put("/admin/costs/{run}") {
                    val body = call.objectBody(); body.checkKeys(setOf("usd_micros", "receipt_reference"))
                    val micros = (body["usd_micros"] as? JsonPrimitive)?.takeUnless { it.isString }?.longOrNull
                        ?: throw Fault(400, "INVALID_COST")
                    val reference = body.string("receipt_reference")
                    val actor = call.subject(); val workspace = call.id("workspace"); val run = call.id("run")
                    db { store.recordConfirmedCost(actor, workspace, run, micros, reference) }
                    call.json(buildJsonObject { put("ok", true) })
                }
                put("/admin/costs/{run}/corrections/{correction}") {
                    val body=call.objectBody()
                    body.checkKeys(setOf("expected_usd_micros","usd_micros","receipt_reference"))
                    fun amount(key:String)=(body[key] as? JsonPrimitive)?.takeUnless { it.isString }?.longOrNull
                        ?: throw Fault(400,"INVALID_COST")
                    val actor=call.subject(); val workspace=call.id("workspace")
                    val run=call.id("run"); val correction=call.id("correction")
                    val previous=amount("expected_usd_micros"); val updated=amount("usd_micros")
                    val receipt=body.string("receipt_reference")
                    db { store.correctConfirmedCost(actor,workspace,run,correction,previous,updated,receipt) }
                    call.json(buildJsonObject { put("ok",true) })
                }
                get("/analysis-settings") {
                    val actor=call.subject(); val workspace=call.id("workspace")
                    val paused=db { store.analysisPaused(actor,workspace) }
                    call.json(buildJsonObject { put("paused",paused) })
                }
                put("/analysis-settings") {
                    val body=call.objectBody(); body.checkKeys(setOf("paused"))
                    val paused=(body["paused"] as? JsonPrimitive)?.takeUnless { it.isString }?.booleanOrNull
                        ?: throw Fault(400,"INVALID_INPUT")
                    val actor=call.subject(); val workspace=call.id("workspace")
                    db { store.setAnalysisPaused(actor,workspace,paused) }
                    call.json(buildJsonObject { put("ok",true) })
                }
                get("/feedback-settings") {
                    val actor = call.subject(); val workspace = call.id("workspace")
                    val enabled = db { store.feedbackEnabled(actor, workspace) }
                    call.json(buildJsonObject { put("enabled", enabled) })
                }
                put("/feedback-settings") {
                    val body = call.objectBody(); body.checkKeys(setOf("enabled"))
                    val enabled = (body["enabled"] as? JsonPrimitive)?.takeUnless { it.isString }?.booleanOrNull
                        ?: throw Fault(400, "INVALID_INPUT")
                    val actor = call.subject(); val workspace = call.id("workspace")
                    db { store.setFeedbackEnabled(actor, workspace, enabled) }
                    call.json(buildJsonObject { put("ok", true) })
                }
                get("/feedback-retention") {
                    val actor = call.subject(); val workspace = call.id("workspace")
                    val days = db { store.feedbackRetention(actor, workspace) }
                    call.json(buildJsonObject { put("days", days) })
                }
                put("/feedback-retention") {
                    val body = call.objectBody(); body.checkKeys(setOf("days"))
                    val days = (body["days"] as? JsonPrimitive)?.takeUnless { it.isString }?.intOrNull
                        ?: throw Fault(400, "INVALID_RETENTION")
                    val actor = call.subject(); val workspace = call.id("workspace")
                    db { store.setFeedbackRetention(actor, workspace, days) }
                    call.json(buildJsonObject { put("ok", true) })
                }
                post("/feedback-retention/purge") {
                    val actor = call.subject(); val workspace = call.id("workspace")
                    val deleted = db { store.purgeExpiredFeedback(actor, workspace) }
                    call.json(buildJsonObject { put("deleted", deleted) })
                }
                put("/feedback/{run}") {
                    val body = call.objectBody(); body.checkKeys(setOf("rating"))
                    val rating = (body["rating"] as? JsonPrimitive)?.takeUnless { it.isString }?.intOrNull
                        ?: throw Fault(400, "INVALID_FEEDBACK")
                    val actor = call.subject(); val workspace = call.id("workspace"); val run = call.id("run")
                    db { store.submitFeedback(actor, workspace, run, rating) }
                    call.json(buildJsonObject { put("ok", true) })
                }
                post("/feedback/{run}/withdraw") {
                    val actor = call.subject(); val workspace = call.id("workspace"); val run = call.id("run")
                    db { store.withdrawFeedback(actor, workspace, run) }
                    call.json(buildJsonObject { put("ok", true) })
                }
                get("/quality-queue") {
                    val actor = call.subject(); val workspace = call.id("workspace")
                    val items = db { store.qualityQueue(actor, workspace) }
                    call.json(buildJsonObject { putJsonArray("items") { items.forEach { item ->
                        add(buildJsonObject {
                            put("run_id", item.runId.toString()); put("rating", item.rating); put("reviewed", item.reviewed)
                        })
                    } } })
                }
                put("/quality-queue/{run}/review") {
                    val actor = call.subject(); val workspace = call.id("workspace"); val run = call.id("run")
                    db { store.markFeedbackReviewed(actor, workspace, run) }
                    call.json(buildJsonObject { put("ok", true) })
                }
            }
            get("/api/v1/workspaces/{workspace}/admin/usage") {
                val actor = call.subject(); val workspace = call.id("workspace")
                val usage = db { store.adminUsage(actor, workspace) }
                call.json(buildJsonObject { usage.forEach { (key, value) -> put(key, value?.let(::JsonPrimitive) ?: JsonNull) } })
            }
        }
    }
}
