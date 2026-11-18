package whitehat.adapters

import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.SQLTransientConnectionException
import java.sql.SQLTimeoutException
import java.security.MessageDigest
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import javax.sql.DataSource
import org.flywaydb.core.Flyway
import whitehat.contracts.Mode
import whitehat.domain.*

class PostgresStore(private val dataSource: DataSource, private val engineHash: String, private val dailyLimit: Int = 20,
    private val statementTimeoutMillis: Int = 10000, private val lockTimeoutMillis: Int = 2000) : DecisionStore {
    init {
        require(engineHash.matches(Regex("[a-f0-9]{64}"))); require(dailyLimit in 1..10000)
        require(statementTimeoutMillis in 1..120000 && lockTimeoutMillis in 1..statementTimeoutMillis)
    }

    companion object {
        fun migrate(source: DataSource): Int = Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate().migrationsExecuted
        fun validate(source: DataSource) {
            val flyway = Flyway.configure().dataSource(source).locations("classpath:db/migration").load()
            flyway.validate()
            check(flyway.info().pending().isEmpty()) { "Pending database migrations; run the migration command first" }
        }
    }

    private fun <T> tx(workspace: UUID? = null, worker: Boolean = false, block: (Connection) -> T): T = try { dataSource.connection.use { c ->
        c.autoCommit = false
        try {
            c.query("SELECT set_config('statement_timeout', ?, true), set_config('lock_timeout', ?, true)",
                statementTimeoutMillis.toString(), lockTimeoutMillis.toString()) { Unit }
            c.query("SELECT set_config('whitehat.workspace', ?, true), set_config('whitehat.worker', ?, true)",
                workspace?.toString().orEmpty(), if (worker) "on" else "off") { Unit }
            val result = block(c)
            c.commit()
            result
        }
        catch (failure: Exception) {
            runCatching { c.rollback() }.exceptionOrNull()?.let(failure::addSuppressed)
            throw failure
        }
    } } catch (failure: SQLException) {
        when {
            failure.sqlState == "23505" -> throw Fault(409, "CONCURRENT_CONFLICT")
            failure is SQLTransientConnectionException || failure.sqlState?.startsWith("08") == true ||
                failure.sqlState in setOf("53300", "57P01", "57P02", "57P03") -> throw Fault(503, "DATABASE_UNAVAILABLE")
            failure is SQLTimeoutException || failure.sqlState in setOf("57014", "55P03", "40P01", "40001") -> throw Fault(503, "DATABASE_BUSY")
            else -> throw failure
        }
    }
    override fun checkReady() { tx { c -> c.query("SELECT 1") { it.getInt(1) }.single().also { check(it == 1) } } }
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
    override fun requireAdministrator(actor: String, workspace: UUID) {
        tx(workspace) { c -> requireRole(c, workspace, actor, setOf(Role.OWNER, Role.ADMIN)) }
    }
    fun requireVoicePilotOwner(actor: String, workspace: UUID) {
        tx(workspace) { c -> requireRole(c, workspace, actor, setOf(Role.OWNER)) }
    }

    // One persistent lifetime allotment for a dedicated pilot workspace.
    fun reserveVoicePilot(actor: String, workspace: UUID) {
        tx(workspace) { c ->
            requireRole(c, workspace, actor, setOf(Role.OWNER))
            val reserved = c.query("""
                INSERT INTO voice_pilot_attempts(workspace_id, attempts) VALUES (?, 1)
                ON CONFLICT (workspace_id) DO UPDATE SET attempts = voice_pilot_attempts.attempts + 1
                  WHERE voice_pilot_attempts.attempts < 3
                RETURNING attempts
            """.trimIndent(), workspace) { it.getInt(1) }
            if (reserved.isEmpty()) throw Fault(429, "VOICE_PILOT_EXHAUSTED")
        }
    }

    fun openVoiceIngress(actor: String, workspace: UUID, session: UUID): UUID = tx(workspace) { c ->
        requireRole(c, workspace, actor, setOf(Role.OWNER))
        val lease = UUID.randomUUID()
        c.exec("""
            INSERT INTO voice_ingress_sessions(workspace_id, session_id, actor_subject, lease_id)
            VALUES (?, ?, ?, ?)
        """.trimIndent(), workspace, session, actor, lease)
        lease
    }

    fun rotateVoiceIngress(actor: String, workspace: UUID, session: UUID, expectedLease: UUID): UUID =
        tx(workspace) { c ->
            requireRole(c, workspace, actor, setOf(Role.OWNER))
            val lease = UUID.randomUUID()
            val changed = c.exec("""
                UPDATE voice_ingress_sessions SET lease_id=?, cursor=0, expires_at=clock_timestamp() + interval '40 seconds'
                WHERE workspace_id=? AND session_id=? AND actor_subject=? AND lease_id=? AND NOT closed
            """.trimIndent(), lease, workspace, session, actor, expectedLease)
            if (changed != 1) throw Fault(409, "VOICE_LEASE_NOT_CURRENT")
            lease
        }

    fun closeVoiceIngress(actor: String, workspace: UUID, session: UUID, lease: UUID) {
        tx(workspace) { c ->
            requireRole(c, workspace, actor, setOf(Role.OWNER))
            c.exec("""
                UPDATE voice_ingress_sessions SET closed=true
                WHERE workspace_id=? AND session_id=? AND actor_subject=? AND lease_id=? AND NOT closed
            """.trimIndent(), workspace, session, actor, lease)
        }
    }

    /** ACK is returned only after the final turn and cursor commit together. */
    fun acceptVoiceFrame(
        actor: String,
        workspace: UUID,
        session: UUID,
        validate: (VoiceCursor) -> VoiceIngressResult,
    ): VoiceIngressResult =
        tx(workspace) { c ->
            requireRole(c, workspace, actor, setOf(Role.OWNER))
            val current = c.query("""
                SELECT lease_id, cursor, closed, expires_at FROM voice_ingress_sessions
                WHERE workspace_id=? AND session_id=? AND actor_subject=? FOR UPDATE
            """.trimIndent(), workspace, session, actor) {
                val state = if (it.getBoolean("closed")) VoiceLeaseState.CLOSED else VoiceLeaseState.ACTIVE
                VoiceCursor(session, it.getObject("lease_id", UUID::class.java), it.getLong("cursor"), state) to
                    it.getObject("expires_at", OffsetDateTime::class.java)
            }.singleOrNull() ?: throw Fault(404, "VOICE_SESSION_UNAVAILABLE")
            val (cursor, expiry) = current
            val expired = c.query("SELECT clock_timestamp() >= ?", expiry) { it.getBoolean(1) }.single()
            if (cursor.state != VoiceLeaseState.ACTIVE || expired) return@tx VoiceIngressResult.Rejected.INACTIVE

            val result = validate(cursor)
            if (result !is VoiceIngressResult.Ready) return@tx result
            check(result.sequence > 0 && result.sequence - 1 == cursor.consumedThrough)

            val effect = result.effect
            val replay = effect is VoiceEffect.FinalTranscript && c.exec("""
                INSERT INTO voice_ingress_turns(workspace_id, session_id, turn_id, transcript)
                VALUES (?, ?, ?, ?) ON CONFLICT (workspace_id, session_id, turn_id) DO NOTHING
            """.trimIndent(), workspace, session, effect.turnId, effect.text) == 0
            check(c.exec("""
                UPDATE voice_ingress_sessions SET cursor=? WHERE workspace_id=? AND session_id=?
            """.trimIndent(), result.sequence, workspace, session) == 1)
            if (replay) VoiceIngressResult.Replay else result
        }

    fun reserveG001Dispatch(actor: String, workspace: UUID, model: String): Boolean =
        tx(workspace) { c ->
            requireRole(c, workspace, actor, setOf(Role.OWNER))
            c.exec("""
                INSERT INTO g001_dispatch(workspace_id, model_id) VALUES (?, ?)
                ON CONFLICT DO NOTHING
            """.trimIndent(), workspace, model) == 1
        }

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
        c.query("SELECT set_config('whitehat.workspace', ?, true)", id.toString()) { Unit }
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
    override fun setMember(actor: String, workspace: UUID, subject: String, role: Role, active: Boolean) = tx(workspace) { c ->
        requireRole(c, workspace, actor, setOf(Role.OWNER))
        if (subject.isBlank() || subject.length > 200 || role == Role.OWNER) throw Fault(400, "INVALID_MEMBER")
        val existing = c.query("SELECT role FROM memberships WHERE workspace_id=? AND subject=? FOR UPDATE", workspace, subject) { it.getString(1) }.singleOrNull()
        if (existing == "OWNER") throw Fault(409, "OWNER_TRANSFER_REQUIRED")
        c.exec("INSERT INTO memberships(workspace_id,subject,role,active) VALUES(?,?,?,?) ON CONFLICT(workspace_id,subject) DO UPDATE SET role=excluded.role,active=excluded.active", workspace, subject, role.name, active)
        if (role != Role.SUPPORT || !active) {
            c.exec("DELETE FROM workspace_operator_permissions WHERE workspace_id=? AND subject=?", workspace, subject)
            c.exec("DELETE FROM workspace_support_assignments WHERE workspace_id=? AND subject=?", workspace, subject)
        }
        audit(c, workspace, actor, "membership.updated", workspace, subject)
    }
    override fun members(actor: String, workspace: UUID): List<WorkspaceMember> = tx(workspace) { c ->
        requireRole(c, workspace, actor, setOf(Role.OWNER))
        c.query("""SELECT m.subject,m.role,m.active,a.role_id,
              ARRAY(SELECT p.permission FROM workspace_operator_permissions p
              WHERE p.workspace_id=m.workspace_id AND p.subject=m.subject ORDER BY p.permission) AS permissions
            FROM memberships m LEFT JOIN workspace_support_assignments a USING (workspace_id,subject)
            WHERE m.workspace_id=? ORDER BY m.active DESC,m.subject LIMIT 100""", workspace) {
            val permissions = (it.getArray("permissions").array as Array<*>).map { value ->
                OperatorPermission.valueOf(value.toString())
            }.toSet()
            WorkspaceMember(it.getString("subject"), Role.valueOf(it.getString("role")), it.getBoolean("active"), permissions, it.getObject("role_id", UUID::class.java))
        }
    }

    override fun setOperatorPermission(actor: String, workspace: UUID, subject: String, permission: OperatorPermission, enabled: Boolean) = tx(workspace) { c ->
        requireRole(c, workspace, actor, setOf(Role.OWNER))
        val role = c.query("SELECT role FROM memberships WHERE workspace_id=? AND subject=? AND active FOR UPDATE",
            workspace, subject) { Role.valueOf(it.getString(1)) }.singleOrNull()
        if (role != Role.SUPPORT) throw Fault(409, "ACTIVE_SUPPORT_REQUIRED")
        val changed = if (enabled) {
            c.exec("INSERT INTO workspace_operator_permissions VALUES (?,?,?) ON CONFLICT DO NOTHING", workspace, subject, permission.name)
        } else {
            c.exec("DELETE FROM workspace_operator_permissions WHERE workspace_id=? AND subject=? AND permission=?", workspace, subject, permission.name)
        }
        if (changed > 0) audit(c, workspace, actor, "permission."+permission.name.lowercase()+
            if (enabled) ".granted" else ".revoked", workspace, subject)
    }

    override fun supportRoles(actor: String, workspace: UUID): List<SupportRole> = tx(workspace) { c ->
        requireRole(c, workspace, actor, setOf(Role.OWNER))
        c.query("""SELECT r.id,r.name, ARRAY(SELECT p.permission FROM workspace_support_role_permissions p
                WHERE p.workspace_id=r.workspace_id AND p.role_id=r.id ORDER BY p.permission) AS permissions
            FROM workspace_support_roles r WHERE r.workspace_id=? ORDER BY r.name,r.id""", workspace) {
            val permissions = (it.getArray("permissions").array as Array<*>)
                .map { value -> OperatorPermission.valueOf(value.toString()) }.toSet()
            SupportRole(it.getObject("id", UUID::class.java), it.getString("name"), permissions)
        }
    }

    override fun saveSupportRole(actor: String, workspace: UUID, role: SupportRole) = tx(workspace) { c ->
        requireRole(c, workspace, actor, setOf(Role.OWNER))
        val name = role.name.trim()
        if (name.isBlank() || name.length > 60 || name.any(Char::isISOControl)) throw Fault(400, "INVALID_ROLE_NAME")
        // Serialize edits and the workspace template limit, including newly inserted ids.
        c.query("SELECT id FROM workspaces WHERE id=? FOR UPDATE", workspace) { Unit }
        val exists = c.query("SELECT id FROM workspace_support_roles WHERE workspace_id=? AND id=?",
            workspace, role.id) { true }.isNotEmpty()
        if (!exists && c.query("SELECT count(*) FROM workspace_support_roles WHERE workspace_id=?",
                workspace) { it.getInt(1) }.single() >= 50) throw Fault(409, "SUPPORT_ROLE_LIMIT")
        c.exec("""INSERT INTO workspace_support_roles VALUES (?,?,?)
            ON CONFLICT(workspace_id,id) DO UPDATE SET name=EXCLUDED.name""", workspace, role.id, name)
        c.exec("DELETE FROM workspace_support_role_permissions WHERE workspace_id=? AND role_id=?", workspace, role.id)
        for (permission in role.permissions) {
            c.exec("INSERT INTO workspace_support_role_permissions VALUES (?,?,?)", workspace, role.id, permission.name)
        }
        audit(c, workspace, actor, "support_role.saved", role.id)
    }

    override fun assignSupportRole(actor: String, workspace: UUID, subject: String, role: UUID?) = tx(workspace) { c ->
        requireRole(c, workspace, actor, setOf(Role.OWNER))
        val activeSupport = c.query("""SELECT subject FROM memberships WHERE workspace_id=? AND subject=?
            AND active AND role='SUPPORT' FOR UPDATE""", workspace, subject) { true }.isNotEmpty()
        if (!activeSupport) throw Fault(409, "ACTIVE_SUPPORT_REQUIRED")
        if (role == null) {
            c.exec("DELETE FROM workspace_support_assignments WHERE workspace_id=? AND subject=?", workspace, subject)
        } else {
            val found = c.query("SELECT id FROM workspace_support_roles WHERE workspace_id=? AND id=? FOR SHARE",
                workspace, role) { true }.isNotEmpty()
            if (!found) throw Fault(404, "NOT_FOUND")
            c.exec("""INSERT INTO workspace_support_assignments VALUES (?,?,?)
                ON CONFLICT(workspace_id,subject) DO UPDATE SET role_id=EXCLUDED.role_id""", workspace, subject, role)
        }
        audit(c, workspace, actor, if (role == null) "support_role.unassigned" else "support_role.assigned", role ?: workspace, subject)
    }

    override fun runHistory(actor: String, workspace: UUID): List<Run> = tx(workspace) { c ->
        requireRole(c, workspace, actor, readers)
        c.query("""SELECT * FROM runs WHERE workspace_id=? AND owner_subject=?
            ORDER BY created_at DESC,id DESC LIMIT 100""", workspace, actor, map = ::runRow)
    }

    private fun requireOperatorAccess(c: Connection, workspace: UUID, actor: String, permission: OperatorPermission) {
        val role = requireRole(c, workspace, actor, setOf(Role.OWNER, Role.SUPPORT))
        if (role == Role.OWNER) return
        val allowed = c.query("""SELECT 1 FROM workspace_operator_permissions
            WHERE workspace_id=? AND subject=? AND permission=? FOR SHARE""", workspace, actor, permission.name) { true }
        if (allowed.isNotEmpty()) return
        // Read and lock the role before a fresh permission query so a concurrent edit is observed.
        val template = c.query("""SELECT r.id FROM workspace_support_assignments a JOIN workspace_support_roles r
            ON r.workspace_id=a.workspace_id AND r.id=a.role_id
            WHERE a.workspace_id=? AND a.subject=? FOR SHARE OF a,r""", workspace, actor) {
            it.getObject(1, UUID::class.java)
        }.singleOrNull() ?: throw Fault(403, "PERMISSION_DENIED")
        if (c.query("""SELECT 1 FROM workspace_support_role_permissions WHERE workspace_id=? AND role_id=?
            AND permission=?""", workspace, template, permission.name) { true }.isEmpty()) throw Fault(403, "PERMISSION_DENIED")
    }

    override fun incidents(actor: String, workspace: UUID): List<RunIncident> = tx(workspace) { c ->
        requireOperatorAccess(c, workspace, actor, OperatorPermission.REVIEW_RUN_INCIDENTS)
        c.query("""SELECT r.id,i.disposition FROM runs r LEFT JOIN run_incident_reviews i
            ON i.workspace_id=r.workspace_id AND i.run_id=r.id
            WHERE r.workspace_id=? AND r.status='reconciliation_required'
            ORDER BY (i.disposition IS NULL) DESC,r.created_at DESC,r.id DESC LIMIT 100""", workspace) {
            RunIncident(it.getObject("id", UUID::class.java), it.getString("disposition")?.let(IncidentDisposition::valueOf))
        }
    }

    override fun reviewIncident(actor: String, workspace: UUID, runId: UUID, disposition: IncidentDisposition) = tx(workspace) { c ->
        requireOperatorAccess(c, workspace, actor, OperatorPermission.REVIEW_RUN_INCIDENTS)
        val exists = c.query("""SELECT 1 FROM runs WHERE workspace_id=? AND id=?
            AND status='reconciliation_required' FOR UPDATE""", workspace, runId) { true }
        if (exists.isEmpty()) throw Fault(404, "NOT_FOUND")
        val changed = c.exec("""INSERT INTO run_incident_reviews(workspace_id,run_id,reviewer_subject,disposition)
            VALUES (?,?,?,?) ON CONFLICT (workspace_id,run_id) DO UPDATE
            SET reviewer_subject=EXCLUDED.reviewer_subject,disposition=EXCLUDED.disposition,reviewed_at=now()
            WHERE run_incident_reviews.disposition IS DISTINCT FROM EXCLUDED.disposition""",
            workspace, runId, actor, disposition.name)
        if(changed>0) audit(c, workspace, actor, "incident."+disposition.name.lowercase(), runId)
    }

    override fun pilotUsage(actor: String, workspace: UUID): PilotUsage = tx(workspace) { c ->
        requireRole(c, workspace, actor, setOf(Role.OWNER, Role.ADMIN))
        // A single statement keeps the meter and source runs on the same MVCC snapshot.
        c.query("""
            WITH scope AS (
                SELECT CAST(? AS uuid) AS workspace_id,
                    date_trunc('month',now() AT TIME ZONE 'UTC')::date AS period_start
            )
            SELECT scope.period_start, entitlement.revision, entitlement.monthly_attempt_limit,
                coalesce(usage.attempts, 0) AS attempts, totals.*
            FROM scope
            LEFT JOIN LATERAL (
                SELECT revision, monthly_attempt_limit FROM workspace_pilot_entitlements
                WHERE workspace_id=scope.workspace_id ORDER BY revision DESC LIMIT 1
            ) entitlement ON true
            LEFT JOIN workspace_pilot_usage usage USING (workspace_id,period_start)
            CROSS JOIN LATERAL (
                SELECT count(*) AS pilot_runs, count(cost.run_id) AS confirmed_cost_runs,
                    sum(cost.usd_micros) AS confirmed_usd_micros
                FROM runs r LEFT JOIN run_confirmed_costs cost ON cost.workspace_id=r.workspace_id AND cost.run_id=r.id
                WHERE r.workspace_id=scope.workspace_id AND r.pilot_entitlement_revision IS NOT NULL
                  AND r.usage_day>=scope.period_start AND r.usage_day<scope.period_start+interval '1 month'
            ) totals
        """, workspace) {
            PilotUsage(
                it.getObject("period_start", LocalDate::class.java),
                it.optionalLong("revision"),
                it.optionalLong("monthly_attempt_limit")?.toInt(),
                it.getLong("attempts"),
                it.getLong("pilot_runs"),
                it.getLong("confirmed_cost_runs"),
                it.getBigDecimal("confirmed_usd_micros")?.toPlainString(),
            )
        }.single()
    }

    override fun confirmedCosts(actor: String, workspace: UUID): Map<String, Long?> = tx(workspace) { c ->
        requireRole(c, workspace, actor, setOf(Role.OWNER, Role.ADMIN))
        c.query("""SELECT count(*) AS runs, count(cost.run_id) AS confirmed_runs,
                sum(cost.usd_micros) AS confirmed_usd_micros
            FROM runs r LEFT JOIN run_confirmed_costs cost ON cost.workspace_id=r.workspace_id AND cost.run_id=r.id
            WHERE r.workspace_id=? AND r.created_at>=now()-interval '30 days'""", workspace) {
            listOf("runs","confirmed_runs","confirmed_usd_micros").associateWith { column -> it.optionalLong(column) }
        }.single()
    }

    override fun operationsMetrics(actor: String, workspace: UUID): Map<String, Long?> = tx(workspace) { c ->
        requireOperatorAccess(c, workspace, actor, OperatorPermission.READ_RUN_METRICS)
        c.query("""SELECT count(*) AS runs,
                count(*) FILTER (WHERE status='failed') AS failed_runs,
                count(*) FILTER (WHERE status='reconciliation_required') AS uncertain_runs,
                count(*) FILTER (WHERE completed_at IS NOT NULL) AS latency_samples,
                percentile_disc(0.95) WITHIN GROUP (ORDER BY
                    greatest(0,extract(epoch FROM (completed_at-created_at))*1000)::bigint)
                    FILTER (WHERE completed_at IS NOT NULL) AS p95_end_to_end_ms
            FROM runs WHERE workspace_id=? AND created_at >= now()-interval '30 days'""", workspace) {
            listOf("runs","failed_runs","uncertain_runs","latency_samples","p95_end_to_end_ms").associateWith { column -> it.optionalLong(column) }
        }.single()
    }

    override fun recordConfirmedCost(actor: String, workspace: UUID, run: UUID, micros: Long, reference: String) = tx(workspace) { c ->
        requireRole(c, workspace, actor, setOf(Role.OWNER))
        if (micros !in 0..1000000000000L || !reference.matches(Regex("[A-Za-z0-9._:-]{1,120}"))) throw Fault(400, "INVALID_COST")
        val terminal = c.query("""SELECT status FROM runs WHERE workspace_id=? AND id=? FOR UPDATE""",
            workspace, run) { it.getString(1) }.singleOrNull() ?: throw Fault(404, "NOT_FOUND")
        if (terminal !in setOf("succeeded","failed","cancelled","reconciliation_required")) throw Fault(409, "RUN_NOT_TERMINAL")
        val prior = c.query("SELECT usd_micros,receipt_reference FROM run_confirmed_costs WHERE workspace_id=? AND run_id=?",
            workspace, run) { it.getLong(1) to it.getString(2) }.singleOrNull()
        if (prior != null && prior != (micros to reference)) throw Fault(409, "COST_ALREADY_RECORDED")
        if (prior == null) {
            c.exec("INSERT INTO run_confirmed_costs VALUES (?,?,?,?)", workspace, run, micros, reference)
            audit(c, workspace, actor, "cost.receipt_recorded", run)
        }
    }

    private fun paused(c: Connection, workspace: UUID): Boolean =
        c.query("SELECT paused FROM workspace_analysis_flags WHERE workspace_id=?", workspace) { it.getBoolean(1) }
            .singleOrNull() ?: false

    override fun analysisPaused(actor: String, workspace: UUID): Boolean = tx(workspace) { c ->
        requireRole(c, workspace, actor, readers)
        paused(c,workspace)
    }

    override fun setAnalysisPaused(actor: String, workspace: UUID, paused: Boolean) = tx(workspace) { c ->
        requireRole(c, workspace, actor, setOf(Role.OWNER))
        c.query("SELECT id FROM workspaces WHERE id=? FOR UPDATE", workspace) { Unit }
        c.exec("""INSERT INTO workspace_analysis_flags VALUES (?,?)
            ON CONFLICT(workspace_id) DO UPDATE SET paused=EXCLUDED.paused""", workspace,paused)
        audit(c, workspace, actor, if(paused) "analysis.paused" else "analysis.resumed", workspace)
    }

    override fun correctConfirmedCost(actor: String, workspace: UUID, run: UUID, correction: UUID,
        previous: Long, amount: Long, receipt: String) = tx(workspace) { c ->
        requireRole(c, workspace, actor, setOf(Role.OWNER))
        if(previous !in 0..1000000000000L || amount !in 0..1000000000000L ||
            !receipt.matches(Regex("[A-Za-z0-9._:-]{1,120}"))) throw Fault(400,"INVALID_COST")
        val current=c.query("""SELECT usd_micros,receipt_reference FROM run_confirmed_costs
            WHERE workspace_id=? AND run_id=? FOR UPDATE""", workspace,run) {
            it.getLong(1) to it.getString(2)
        }.singleOrNull() ?: throw Fault(404,"NOT_FOUND")
        val repeated=c.query("""SELECT previous_micros,corrected_micros,corrected_receipt FROM run_cost_corrections
            WHERE workspace_id=? AND run_id=? AND correction_id=?""", workspace,run,correction) {
            Triple(it.getLong(1),it.getLong(2),it.getString(3))
        }.singleOrNull()
        if(repeated!=null){
            if(repeated!=Triple(previous,amount,receipt)) throw Fault(409,"COST_REVISION_CONFLICT")
            return@tx
        }
        if(current.first!=previous) throw Fault(409,"COST_REVISION_CONFLICT")
        c.exec("""INSERT INTO run_cost_corrections
            (workspace_id,run_id,correction_id,previous_micros,corrected_micros,previous_receipt,corrected_receipt,actor_subject)
            VALUES (?,?,?,?,?,?,?,?)""",workspace,run,correction,previous,amount,current.second,receipt,actor)
        c.exec("UPDATE run_confirmed_costs SET usd_micros=?,receipt_reference=? WHERE workspace_id=? AND run_id=?",
            amount,receipt,workspace,run)
        audit(c,workspace,actor,"cost.receipt_corrected",run)
    }

    private fun readFeedbackEnabled(c: Connection, workspace: UUID): Boolean =
        c.query("SELECT feedback_enabled FROM workspace_quality_settings WHERE workspace_id=? FOR SHARE",
            workspace) { it.getBoolean(1) }.singleOrNull() ?: false

    override fun feedbackEnabled(actor: String, workspace: UUID): Boolean = tx(workspace) { c ->
        requireRole(c, workspace, actor, readers)
        readFeedbackEnabled(c, workspace)
    }

    override fun setFeedbackEnabled(actor: String, workspace: UUID, enabled: Boolean) = tx(workspace) { c ->
        requireRole(c, workspace, actor, setOf(Role.OWNER))
        c.exec("""INSERT INTO workspace_quality_settings(workspace_id,feedback_enabled) VALUES (?,?) ON CONFLICT(workspace_id) DO UPDATE
            SET feedback_enabled=EXCLUDED.feedback_enabled""", workspace, enabled)
        audit(c, workspace, actor, if (enabled) "feedback.enabled" else "feedback.disabled", workspace)
    }

    override fun submitFeedback(actor: String, workspace: UUID, run: UUID, rating: Int) = tx(workspace) { c ->
        requireRole(c, workspace, actor, readers)
        if (rating !in 1..5) throw Fault(400, "INVALID_FEEDBACK")
        if (!readFeedbackEnabled(c, workspace)) throw Fault(409, "FEEDBACK_DISABLED")
        val owned = readRun(c, workspace, actor, run, true)
        if (owned.status != "succeeded") throw Fault(409, "RUN_NOT_SUCCEEDED")
        c.exec("""INSERT INTO run_quality_feedback(workspace_id,run_id,rating) VALUES (?,?,?)
            ON CONFLICT(workspace_id,run_id) DO UPDATE SET rating=EXCLUDED.rating,reviewed=false,submitted_at=now()
            WHERE run_quality_feedback.rating IS DISTINCT FROM EXCLUDED.rating""", workspace, run, rating)
        Unit
    }

    override fun withdrawFeedback(actor: String, workspace: UUID, run: UUID) = tx(workspace) { c ->
        requireRole(c, workspace, actor, readers)
        readRun(c, workspace, actor, run, true)
        c.exec("DELETE FROM run_quality_feedback WHERE workspace_id=? AND run_id=?", workspace, run)
        Unit
    }

    override fun feedbackRetention(actor: String, workspace: UUID): Int? = tx(workspace) { c ->
        requireRole(c, workspace, actor, setOf(Role.OWNER))
        c.query("SELECT retention_days FROM workspace_quality_settings WHERE workspace_id=?", workspace) {
            it.getObject(1, Integer::class.java)?.toInt()
        }.singleOrNull()
    }

    override fun setFeedbackRetention(actor: String, workspace: UUID, days: Int) = tx(workspace) { c ->
        requireRole(c, workspace, actor, setOf(Role.OWNER))
        if (days !in 1..365) throw Fault(400, "INVALID_RETENTION")
        c.exec("""INSERT INTO workspace_quality_settings(workspace_id,feedback_enabled,retention_days)
            VALUES (?,false,?) ON CONFLICT(workspace_id) DO UPDATE SET retention_days=EXCLUDED.retention_days""", workspace, days)
        audit(c, workspace, actor, "feedback.retention_changed", workspace)
    }

    override fun purgeExpiredFeedback(actor: String, workspace: UUID): Int = tx(workspace) { c ->
        requireRole(c, workspace, actor, setOf(Role.OWNER))
        val days = c.query("SELECT retention_days FROM workspace_quality_settings WHERE workspace_id=? FOR SHARE", workspace) {
            it.getObject(1, Integer::class.java)?.toInt()
        }.singleOrNull() ?: throw Fault(409, "RETENTION_UNCONFIGURED")
        val count = c.exec("""DELETE FROM run_quality_feedback WHERE workspace_id=?
            AND submitted_at < now()-make_interval(days => ?)""", workspace, days)
        if (count > 0) audit(c, workspace, actor, "feedback.expired_purged", workspace)
        count
    }

    fun sweepExpiredFeedback(): Int {
        val workspaces = tx(worker=true) { c ->
            c.query("SELECT workspace_id FROM workspace_quality_settings WHERE retention_days IS NOT NULL") {
                it.getObject(1,UUID::class.java)
            }
        }
        return workspaces.sumOf { workspace -> tx(workspace) { c ->
            val days=c.query("SELECT retention_days FROM workspace_quality_settings WHERE workspace_id=? FOR SHARE",
                workspace) { it.getObject(1,Integer::class.java)?.toInt() }.singleOrNull() ?: return@tx 0
            val deleted=c.exec("DELETE FROM run_quality_feedback WHERE workspace_id=? AND submitted_at < now()-make_interval(days => ?)",
                workspace,days)
            if(deleted>0) audit(c,workspace,"system:feedback_retention","feedback.expired_purged",workspace)
            deleted
        } }
    }

    override fun qualityQueue(actor: String, workspace: UUID): List<QualityFeedback> = tx(workspace) { c ->
        requireOperatorAccess(c, workspace, actor, OperatorPermission.REVIEW_QUALITY_FEEDBACK)
        c.query("""SELECT f.run_id,f.rating,f.reviewed FROM run_quality_feedback f
            LEFT JOIN workspace_quality_settings s USING(workspace_id) WHERE f.workspace_id=?
            AND (s.retention_days IS NULL OR f.submitted_at >= now()-make_interval(days => s.retention_days))
            ORDER BY reviewed,submitted_at DESC,run_id LIMIT 100""", workspace) {
            QualityFeedback(it.getObject("run_id", UUID::class.java), it.getInt("rating"), it.getBoolean("reviewed"))
        }
    }

    override fun markFeedbackReviewed(actor: String, workspace: UUID, run: UUID) = tx(workspace) { c ->
        requireOperatorAccess(c, workspace, actor, OperatorPermission.REVIEW_QUALITY_FEEDBACK)
        val changed = c.exec("""UPDATE run_quality_feedback SET reviewed=true
            WHERE workspace_id=? AND run_id=? AND NOT reviewed""", workspace, run)
        if (changed>0) audit(c, workspace, actor, "feedback.reviewed", run)
        else if (c.query("SELECT 1 FROM run_quality_feedback WHERE workspace_id=? AND run_id=?", workspace, run) { true }.isEmpty()) {
            throw Fault(404, "NOT_FOUND")
        }
    }

    override fun createDecision(subject: String, workspace: UUID, input: DecisionInput): Decision = tx(workspace) { c ->
        input.validate(); requireRole(c, workspace, subject, readers)
        val d = Decision(UUID.randomUUID(), workspace, subject, 1, input)
        c.exec("INSERT INTO decisions(workspace_id,id,owner_subject,title,decision,objective,context,source_notes,mode) VALUES(?,?,?,?,?,?,?,?,?)", workspace, d.id, subject, input.title, input.decision, input.objective, input.context, input.sourceNotes, input.mode.wireId)
        audit(c, workspace, subject, "decision.created", d.id); d
    }
    override fun decisions(subject: String, workspace: UUID): List<Decision> = tx(workspace) { c ->
        requireRole(c, workspace, subject, readers)
        c.query("SELECT * FROM decisions WHERE workspace_id=? AND owner_subject=? ORDER BY created_at DESC,id DESC LIMIT 100", workspace, subject, map = ::decisionRow)
    }
    override fun decision(subject: String, workspace: UUID, id: UUID): Decision = tx(workspace) { c ->
        requireRole(c, workspace, subject, readers); readDecision(c, workspace, subject, id)
    }
    private fun assertIdle(c: Connection, workspace: UUID, id: UUID) {
        if (c.query("SELECT id FROM runs WHERE workspace_id=? AND decision_id=? AND status IN ('queued','running','cancellation_requested','reconciliation_required') LIMIT 1", workspace, id) { it.getObject(1) }.isNotEmpty()) throw Fault(409, "ACTIVE_RUN_EXISTS")
    }
    override fun editDecision(subject: String, workspace: UUID, id: UUID, expectedRevision: Long, input: DecisionInput): Decision = tx(workspace) { c ->
        input.validate(); requireRole(c, workspace, subject, readers)
        val previous = readDecision(c, workspace, subject, id, true)
        if (previous.revision != expectedRevision) throw Fault(409, "REVISION_CONFLICT")
        assertIdle(c, workspace, id)
        c.exec("UPDATE decisions SET title=?,decision=?,objective=?,context=?,source_notes=?,mode=?,revision=revision+1,updated_at=now() WHERE workspace_id=? AND id=?", input.title, input.decision, input.objective, input.context, input.sourceNotes, input.mode.wireId, workspace, id)
        audit(c, workspace, subject, "decision.updated", id); previous.copy(revision = previous.revision + 1, input = input)
    }
    private fun reservePilotAttempt(c: Connection, workspace: UUID): Long? {
        val (revision, limit) = c.query("""
            SELECT revision, monthly_attempt_limit FROM workspace_pilot_entitlements
            WHERE workspace_id=? ORDER BY revision DESC LIMIT 1
        """, workspace) { it.getLong("revision") to it.getInt("monthly_attempt_limit") }.singleOrNull() ?: return null
        if (limit == 0) throw Fault(429, "PILOT_LIMIT_REACHED")
        val reserved = c.query("""
            INSERT INTO workspace_pilot_usage(workspace_id,period_start,attempts)
            VALUES (?,date_trunc('month',now() AT TIME ZONE 'UTC')::date,1)
            ON CONFLICT (workspace_id,period_start) DO UPDATE SET attempts=workspace_pilot_usage.attempts+1
            WHERE workspace_pilot_usage.attempts < ? RETURNING attempts
        """, workspace, limit) { it.getInt(1) }
        if (reserved.isEmpty()) throw Fault(429, "PILOT_LIMIT_REACHED")
        return revision
    }

    override fun enqueue(subject: String, workspace: UUID, id: UUID, expectedRevision: Long, key: String): Run = tx(workspace) { c ->
        if (!key.matches(Regex("[A-Za-z0-9_-]{8,120}")) || expectedRevision < 1) throw Fault(400, "INVALID_RUN_REQUEST")
        requireRole(c, workspace, subject, readers)
        val d = readDecision(c, workspace, subject, id, true)
        val hash = MessageDigest.getInstance("SHA-256").digest("$id:$expectedRevision".toByteArray()).joinToString("") { "%02x".format(it) }
        val previous = c.query("SELECT * FROM runs WHERE workspace_id=? AND owner_subject=? AND idempotency_key=?", workspace, subject, key) { it.getString("request_hash") to runRow(it) }.singleOrNull()
        if (previous != null) {
            if (previous.first != hash) throw Fault(409, "IDEMPOTENCY_CONFLICT")
            return@tx previous.second
        }
        c.query("SELECT id FROM workspaces WHERE id=? FOR SHARE",workspace) { Unit }
        if(paused(c,workspace)) throw Fault(409,"ANALYSIS_PAUSED")
        if (d.revision != expectedRevision) throw Fault(409, "REVISION_CONFLICT")
        if (d.input.mode == Mode.COMPARE && d.input.objective.isBlank()) throw Fault(400, "COMPARE_OBJECTIVE_REQUIRED")
        assertIdle(c, workspace, id)
        val reserved = c.query("INSERT INTO usage_counters(workspace_id,subject,usage_day,attempts) VALUES(?,?,(now() AT TIME ZONE 'UTC')::date,1) ON CONFLICT(workspace_id,subject,usage_day) DO UPDATE SET attempts=usage_counters.attempts+1 WHERE usage_counters.attempts < ? RETURNING attempts", workspace, subject, dailyLimit) { it.getInt(1) }
        if (reserved.isEmpty()) throw Fault(429, "DAILY_LIMIT_REACHED")
        val pilotRevision = reservePilotAttempt(c, workspace)
        val runId = UUID.randomUUID()
        c.exec("""INSERT INTO runs(workspace_id,id,decision_id,owner_subject,input_revision,idempotency_key,
            request_hash,snapshot_json,status,engine_hash,pilot_entitlement_revision)
            VALUES (?,?,?,?,?,?,?,?::jsonb,'queued',?,?)""",
            workspace, runId, id, subject, expectedRevision, key, hash, d.encode(), engineHash, pilotRevision)
        audit(c, workspace, subject, "run.queued", runId); readRun(c, workspace, subject, runId)
    }
    override fun run(subject: String, workspace: UUID, id: UUID): Run = tx(workspace) { c ->
        requireRole(c, workspace, subject, readers); readRun(c, workspace, subject, id)
    }
    override fun cancel(subject: String, workspace: UUID, id: UUID): Run = tx(workspace) { c ->
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
    override fun recoverExpired(): Int = tx(worker = true) { c -> reconcileExpired(c) }
    override fun claim(leaseSeconds: Long): Claim? = tx(worker = true) { c ->
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
    override fun complete(claim: Claim, completion: Completion) = tx(worker = true) { c ->
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
    override fun adminUsage(subject: String, workspace: UUID): Map<String, Long?> = tx(workspace) { c ->
        requireRole(c, workspace, subject, setOf(Role.OWNER, Role.ADMIN))
        c.query("SELECT count(*) AS runs,sum(input_tokens) AS input_tokens,sum(output_tokens) AS output_tokens,count(*) FILTER(WHERE status='reconciliation_required') AS uncertain FROM runs WHERE workspace_id=?", workspace) {
            mapOf("runs" to it.getLong("runs"), "input_tokens" to it.optionalLong("input_tokens"), "output_tokens" to it.optionalLong("output_tokens"), "uncertain_runs" to it.getLong("uncertain"))
        }.single()
    }
}
