package whitehat.adapters

import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.security.MessageDigest
import java.util.UUID
import javax.sql.DataSource
import org.flywaydb.core.Flyway
import whitehat.contracts.Mode
import whitehat.domain.*

class PostgresStore(private val dataSource: DataSource, private val engineHash: String, private val dailyLimit: Int = 20) : DecisionStore {
    init { require(engineHash.matches(Regex("[a-f0-9]{64}"))); require(dailyLimit in 1..10000) }

    companion object {
        fun migrate(source: DataSource): Int = Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate().migrationsExecuted
        fun validate(source: DataSource) {
            val flyway = Flyway.configure().dataSource(source).locations("classpath:db/migration").load()
            flyway.validate()
            check(flyway.info().pending().isEmpty()) { "Pending database migrations; run the migration command first" }
        }
    }

    private fun <T> tx(block: (Connection) -> T): T = dataSource.connection.use { c ->
        c.autoCommit = false
        try { val result = block(c); c.commit(); result }
        catch (failure: Exception) {
            c.rollback()
            if (failure is SQLException && failure.sqlState == "23505") throw Fault(409, "CONCURRENT_CONFLICT")
            throw failure
        }
    }
    private fun <T> Connection.query(sql: String, vararg values: Any?, map: (ResultSet) -> T): List<T> = prepareStatement(sql).use { statement ->
        values.forEachIndexed { i, v -> statement.setObject(i + 1, v) }
        statement.executeQuery().use { rows -> buildList { while (rows.next()) add(map(rows)) } }
    }
    private fun Connection.exec(sql: String, vararg values: Any?): Int = prepareStatement(sql).use { statement ->
        values.forEachIndexed { i, v -> statement.setObject(i + 1, v) }; statement.executeUpdate()
    }
    private fun audit(c: Connection, workspace: UUID, subject: String, action: String, target: UUID, targetSubject: String? = null) {
        c.exec("INSERT INTO audit(id,workspace_id,actor_subject,action,target_id,target_subject) VALUES(?,?,?,?,?,?)", UUID.randomUUID(), workspace, subject, action, target, targetSubject)
    }
    private fun requireRole(c: Connection, workspace: UUID, subject: String, allowed: Set<Role>): Role {
        val role = c.query("SELECT role FROM memberships WHERE workspace_id=? AND subject=? AND active FOR SHARE", workspace, subject) { Role.valueOf(it.getString(1)) }.singleOrNull() ?: throw Fault(404, "NOT_FOUND")
        if (role !in allowed) throw Fault(403, "PERMISSION_DENIED")
        return role
    }
    private val readers = setOf(Role.OWNER, Role.ADMIN, Role.MEMBER)
    private fun readDecision(c: Connection, workspace: UUID, subject: String, id: UUID, lock: Boolean = false): Decision =
        c.query("SELECT * FROM decisions WHERE workspace_id=? AND owner_subject=? AND id=?" + if (lock) " FOR UPDATE" else "", workspace, subject, id, map = ::decisionRow).singleOrNull() ?: throw Fault(404, "NOT_FOUND")

    private fun decisionRow(r: ResultSet) = Decision(r.getObject("id", UUID::class.java), r.getObject("workspace_id", UUID::class.java), r.getString("owner_subject"), r.getLong("revision"),
        DecisionInput(r.getString("title"), r.getString("decision"), r.getString("objective"), r.getString("context"), r.getString("source_notes"), Mode.fromWire(r.getString("mode"))!!))
    private fun ResultSet.optionalLong(key: String): Long? { val value = getLong(key); return if (wasNull()) null else value }
    private fun runRow(r: ResultSet) = Run(r.getObject("id", UUID::class.java), r.getObject("workspace_id", UUID::class.java), r.getObject("decision_id", UUID::class.java), r.getLong("input_revision"),
        r.getString("status"), r.getString("result_json"), r.getString("error_code"), r.optionalLong("input_tokens"), r.optionalLong("output_tokens"))
    private fun readRun(c: Connection, workspace: UUID, subject: String, id: UUID, lock: Boolean = false): Run =
        c.query("SELECT * FROM runs WHERE workspace_id=? AND owner_subject=? AND id=?" + if (lock) " FOR UPDATE" else "", workspace, subject, id, map = ::runRow).singleOrNull() ?: throw Fault(404, "NOT_FOUND")

    override fun createWorkspace(subject: String, name: String): Workspace = tx { c ->
        if (name.isBlank() || name.length > 120) throw Fault(400, "INVALID_INPUT")
        val id = UUID.randomUUID()
        c.exec("INSERT INTO workspaces(id,name) VALUES(?,?)", id, name.trim())
        c.exec("INSERT INTO memberships(workspace_id,subject,role) VALUES(?,?,'OWNER')", id, subject)
        audit(c, id, subject, "workspace.created", id)
        Workspace(id, name.trim(), Role.OWNER)
    }
    override fun workspaces(subject: String): List<Workspace> = tx { c ->
        c.query("SELECT w.id,w.name,m.role FROM workspaces w JOIN memberships m ON m.workspace_id=w.id WHERE m.subject=? AND m.active ORDER BY w.created_at,w.id LIMIT 100", subject) {
            Workspace(it.getObject("id", UUID::class.java), it.getString("name"), Role.valueOf(it.getString("role")))
        }
    }
    override fun setMember(actor: String, workspace: UUID, subject: String, role: Role, active: Boolean) = tx { c ->
        requireRole(c, workspace, actor, setOf(Role.OWNER))
        if (subject.isBlank() || subject.length > 200 || role == Role.OWNER) throw Fault(400, "INVALID_MEMBER")
        val existing = c.query("SELECT role FROM memberships WHERE workspace_id=? AND subject=? FOR UPDATE", workspace, subject) { it.getString(1) }.singleOrNull()
        if (existing == "OWNER") throw Fault(409, "OWNER_TRANSFER_REQUIRED")
        c.exec("INSERT INTO memberships(workspace_id,subject,role,active) VALUES(?,?,?,?) ON CONFLICT(workspace_id,subject) DO UPDATE SET role=excluded.role,active=excluded.active", workspace, subject, role.name, active)
        audit(c, workspace, actor, "membership.updated", workspace, subject)
    }
    override fun createDecision(subject: String, workspace: UUID, input: DecisionInput): Decision = tx { c ->
        input.validate(); requireRole(c, workspace, subject, readers)
        val d = Decision(UUID.randomUUID(), workspace, subject, 1, input)
        c.exec("INSERT INTO decisions(workspace_id,id,owner_subject,title,decision,objective,context,source_notes,mode) VALUES(?,?,?,?,?,?,?,?,?)", workspace, d.id, subject, input.title, input.decision, input.objective, input.context, input.sourceNotes, input.mode.wireId)
        audit(c, workspace, subject, "decision.created", d.id); d
    }
    override fun decisions(subject: String, workspace: UUID): List<Decision> = tx { c ->
        requireRole(c, workspace, subject, readers)
        c.query("SELECT * FROM decisions WHERE workspace_id=? AND owner_subject=? ORDER BY created_at DESC,id DESC LIMIT 100", workspace, subject, map = ::decisionRow)
    }
    override fun decision(subject: String, workspace: UUID, id: UUID): Decision = tx { c ->
        requireRole(c, workspace, subject, readers); readDecision(c, workspace, subject, id)
    }
    private fun assertIdle(c: Connection, workspace: UUID, id: UUID) {
        if (c.query("SELECT id FROM runs WHERE workspace_id=? AND decision_id=? AND status IN ('queued','running','cancellation_requested','reconciliation_required') LIMIT 1", workspace, id) { it.getObject(1) }.isNotEmpty()) throw Fault(409, "ACTIVE_RUN_EXISTS")
    }
    override fun editDecision(subject: String, workspace: UUID, id: UUID, expectedRevision: Long, input: DecisionInput): Decision = tx { c ->
        input.validate(); requireRole(c, workspace, subject, readers)
        val previous = readDecision(c, workspace, subject, id, true)
        if (previous.revision != expectedRevision) throw Fault(409, "REVISION_CONFLICT")
        assertIdle(c, workspace, id)
        c.exec("UPDATE decisions SET title=?,decision=?,objective=?,context=?,source_notes=?,mode=?,revision=revision+1,updated_at=now() WHERE workspace_id=? AND id=?", input.title, input.decision, input.objective, input.context, input.sourceNotes, input.mode.wireId, workspace, id)
        audit(c, workspace, subject, "decision.updated", id); previous.copy(revision = previous.revision + 1, input = input)
    }
    override fun enqueue(subject: String, workspace: UUID, id: UUID, expectedRevision: Long, key: String): Run = tx { c ->
        if (!key.matches(Regex("[A-Za-z0-9_-]{8,120}")) || expectedRevision < 1) throw Fault(400, "INVALID_RUN_REQUEST")
        requireRole(c, workspace, subject, readers)
        val d = readDecision(c, workspace, subject, id, true)
        val hash = MessageDigest.getInstance("SHA-256").digest("$id:$expectedRevision".toByteArray()).joinToString("") { "%02x".format(it) }
        val previous = c.query("SELECT * FROM runs WHERE workspace_id=? AND owner_subject=? AND idempotency_key=?", workspace, subject, key) { it.getString("request_hash") to runRow(it) }.singleOrNull()
        if (previous != null) {
            if (previous.first != hash) throw Fault(409, "IDEMPOTENCY_CONFLICT")
            return@tx previous.second
        }
        if (d.revision != expectedRevision) throw Fault(409, "REVISION_CONFLICT")
        if (d.input.mode == Mode.COMPARE && d.input.objective.isBlank()) throw Fault(400, "COMPARE_OBJECTIVE_REQUIRED")
        assertIdle(c, workspace, id)
        val reserved = c.query("INSERT INTO usage_counters(workspace_id,subject,usage_day,attempts) VALUES(?,?,(now() AT TIME ZONE 'UTC')::date,1) ON CONFLICT(workspace_id,subject,usage_day) DO UPDATE SET attempts=usage_counters.attempts+1 WHERE usage_counters.attempts < ? RETURNING attempts", workspace, subject, dailyLimit) { it.getInt(1) }
        if (reserved.isEmpty()) throw Fault(429, "DAILY_LIMIT_REACHED")
        val runId = UUID.randomUUID()
        c.exec("INSERT INTO runs(workspace_id,id,decision_id,owner_subject,input_revision,idempotency_key,request_hash,snapshot_json,status,engine_hash) VALUES(?,?,?,?,?,?,?,?::jsonb,'queued',?)", workspace, runId, id, subject, expectedRevision, key, hash, d.encode(), engineHash)
        audit(c, workspace, subject, "run.queued", runId); readRun(c, workspace, subject, runId)
    }
    override fun run(subject: String, workspace: UUID, id: UUID): Run = tx { c ->
        requireRole(c, workspace, subject, readers); readRun(c, workspace, subject, id)
    }
    override fun cancel(subject: String, workspace: UUID, id: UUID): Run = tx { c ->
        requireRole(c, workspace, subject, readers)
        val previous = readRun(c, workspace, subject, id, true)
        val next = when (previous.status) { "queued" -> "cancelled"; "running" -> "cancellation_requested"; "cancelled", "cancellation_requested" -> previous.status; else -> throw Fault(409, "RUN_ALREADY_TERMINAL") }
        if (previous.status != next) {
            c.exec("UPDATE runs SET status=?,completed_at=CASE WHEN ?='cancelled' THEN now() ELSE NULL END WHERE workspace_id=? AND id=?", next, next, workspace, id)
            audit(c, workspace, subject, "run.cancel_requested", id)
        }
        readRun(c, workspace, subject, id)
    }
    private fun reconcileExpired(c: Connection): Int {
        val expired = c.query("UPDATE runs SET status='reconciliation_required',error_code='WORKER_LEASE_EXPIRED',completed_at=now() WHERE status IN ('running','cancellation_requested') AND lease_until < clock_timestamp() RETURNING workspace_id,id") { it.getObject(1, UUID::class.java) to it.getObject(2, UUID::class.java) }
        for ((workspace, id) in expired) audit(c, workspace, "system:worker", "run.reconciliation_required", id)
        return expired.size
    }
    override fun recoverExpired(): Int = tx { c -> reconcileExpired(c) }
    override fun claim(leaseSeconds: Long): Claim? = tx { c ->
        require(leaseSeconds in 1..300)
        reconcileExpired(c)
        val row = c.query("SELECT * FROM runs WHERE status='queued' ORDER BY created_at,id FOR UPDATE SKIP LOCKED LIMIT 1") { runRow(it) to it.getString("snapshot_json") }.singleOrNull() ?: return@tx null
        val (run, snapshot) = row
        val authorized = c.query("SELECT 1 FROM memberships WHERE workspace_id=? AND subject=? AND active AND role IN ('OWNER','ADMIN','MEMBER')", run.workspace, decodeDecision(snapshot).owner) { it.getInt(1) }.isNotEmpty()
        if (!authorized) {
            c.exec("UPDATE runs SET status='failed',error_code='MEMBERSHIP_REVOKED',completed_at=now() WHERE workspace_id=? AND id=?", run.workspace, run.id)
            audit(c, run.workspace, "system:worker", "run.membership_revoked", run.id); return@tx null
        }
        val token = UUID.randomUUID()
        c.exec("UPDATE runs SET status='running',lease_token=?,lease_until=clock_timestamp() + (? * interval '1 second'),attempts=attempts+1 WHERE workspace_id=? AND id=?", token, leaseSeconds, run.workspace, run.id)
        audit(c, run.workspace, "system:worker", "run.started", run.id)
        Claim(run.copy(status = "running"), token, decodeDecision(snapshot))
    }
    override fun complete(claim: Claim, completion: Completion) = tx { c ->
        val run = claim.run
        val current = c.query("SELECT status,lease_token,lease_until>clock_timestamp() AS live FROM runs WHERE workspace_id=? AND id=? FOR UPDATE", run.workspace, run.id) {
            Triple(it.getString("status"), it.getObject("lease_token", UUID::class.java), it.getBoolean("live"))
        }.singleOrNull() ?: return@tx
        // The receipt remains useful even after cancellation or lease expiry.
        completion.receipt?.let { receipt ->
            c.exec("INSERT INTO provider_receipts(workspace_id,run_id,lease_token,model,provider_request_id,input_tokens,output_tokens) VALUES(?,?,?,?,?,?,?) ON CONFLICT DO NOTHING", run.workspace, run.id, claim.token, receipt.model, receipt.providerId, receipt.inputTokens, receipt.outputTokens)
            c.exec("UPDATE runs r SET model=coalesce(r.model,p.model),provider_request_id=coalesce(r.provider_request_id,p.provider_request_id),input_tokens=coalesce(r.input_tokens,p.input_tokens),output_tokens=coalesce(r.output_tokens,p.output_tokens) FROM provider_receipts p WHERE r.workspace_id=p.workspace_id AND r.id=p.run_id AND r.workspace_id=? AND r.id=? AND p.lease_token=? AND r.lease_token=p.lease_token", run.workspace, run.id, claim.token)
        }
        if (current.second != claim.token || current.first !in setOf("running", "cancellation_requested")) return@tx
        val next = when {
            !current.third || completion.uncertain -> "reconciliation_required"
            current.first == "cancellation_requested" -> "cancelled"
            completion.error != null -> "failed"
            completion.output != null -> "succeeded"
            else -> "reconciliation_required"
        }
        val receipt = completion.receipt
        c.exec("UPDATE runs SET status=?,result_json=?::jsonb,error_code=?,model=?,provider_request_id=?,input_tokens=?,output_tokens=?,completed_at=now() WHERE workspace_id=? AND id=?", next, if (next == "succeeded") completion.output else null,
            if (!current.third) "WORKER_LEASE_EXPIRED" else completion.error, receipt?.model, receipt?.providerId, receipt?.inputTokens, receipt?.outputTokens, run.workspace, run.id)
        audit(c, run.workspace, "system:worker", "run.$next", run.id)
    }
    override fun adminUsage(subject: String, workspace: UUID): Map<String, Long?> = tx { c ->
        requireRole(c, workspace, subject, setOf(Role.OWNER, Role.ADMIN))
        c.query("SELECT count(*) AS runs,sum(input_tokens) AS input_tokens,sum(output_tokens) AS output_tokens,count(*) FILTER(WHERE status='reconciliation_required') AS uncertain FROM runs WHERE workspace_id=?", workspace) {
            mapOf("runs" to it.getLong("runs"), "input_tokens" to it.optionalLong("input_tokens"), "output_tokens" to it.optionalLong("output_tokens"), "uncertain_runs" to it.getLong("uncertain"))
        }.single()
    }
}
