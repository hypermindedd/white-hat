package whitehat.domain

import java.time.LocalDate
import java.util.UUID
import whitehat.contracts.Mode

class Fault(val status: Int, val code: String) : RuntimeException(code)
enum class Role { OWNER, ADMIN, MEMBER, SUPPORT }
enum class OperatorPermission { REVIEW_RUN_INCIDENTS, READ_RUN_METRICS, REVIEW_QUALITY_FEEDBACK }
data class SupportRole(val id: UUID, val name: String, val permissions: Set<OperatorPermission>)
data class QualityFeedback(val runId: UUID, val rating: Int, val reviewed: Boolean)
enum class IncidentDisposition { FOLLOW_UP_REQUIRED, RECEIPT_REVIEWED }
data class WorkspaceMember(val subject: String, val role: Role, val active: Boolean, val permissions: Set<OperatorPermission>, val supportRoleId: UUID?) {
    val canReviewIncidents get() = OperatorPermission.REVIEW_RUN_INCIDENTS in permissions
}
data class RunIncident(val runId: UUID, val disposition: IncidentDisposition?)
data class Workspace(val id: UUID, val name: String, val role: Role)
data class PilotUsage(
    val periodStart: LocalDate,
    val entitlementRevision: Long?,
    val attemptLimit: Int?,
    val meteredAttempts: Long,
    val pilotRuns: Long,
    val confirmedCostRuns: Long,
    val confirmedUsdMicros: String?,
) {
    val reconciled get() = meteredAttempts == pilotRuns
}
data class DecisionInput(val title: String, val decision: String, val objective: String = "", val context: String = "", val sourceNotes: String = "", val mode: Mode = Mode.CHALLENGE) {
    fun validate() {
        if (title.isBlank() || title.length > 120 || decision.isBlank() || decision.length > 16000 || objective.length > 2000 || context.length > 12000 || sourceNotes.length > 10000) throw Fault(400, "INVALID_INPUT")
    }
}
data class Decision(val id: UUID, val workspace: UUID, val owner: String, val revision: Long, val input: DecisionInput)
data class Run(val id: UUID, val workspace: UUID, val decision: UUID, val revision: Long, val status: String, val result: String?, val error: String?, val inputTokens: Long?, val outputTokens: Long?)
data class Claim(val run: Run, val token: UUID, val snapshot: Decision)
data class Receipt(val model: String?, val providerId: String?, val inputTokens: Long?, val outputTokens: Long?)
data class ProviderResponse(val output: String, val receipt: Receipt)
class ProviderFailure(val code: String, val uncertain: Boolean = false, val receipt: Receipt? = null) : RuntimeException(code)
data class Completion(val output: String? = null, val error: String? = null, val uncertain: Boolean = false, val receipt: Receipt? = null)

interface DecisionProvider {
    val configured: Boolean
    suspend fun analyze(decision: Decision): ProviderResponse
}

/** Only a server repository adapter implements durable writes. */
interface DecisionStore {
    fun createWorkspace(subject: String, name: String): Workspace
    fun workspaces(subject: String): List<Workspace>
    fun setMember(actor: String, workspace: UUID, subject: String, role: Role, active: Boolean)
    fun members(actor: String, workspace: UUID): List<WorkspaceMember>
    fun setIncidentPermission(actor: String, workspace: UUID, subject: String, enabled: Boolean) =
        setOperatorPermission(actor, workspace, subject, OperatorPermission.REVIEW_RUN_INCIDENTS, enabled)
    fun setOperatorPermission(actor: String, workspace: UUID, subject: String, permission: OperatorPermission, enabled: Boolean)
    fun supportRoles(actor: String, workspace: UUID): List<SupportRole>
    fun saveSupportRole(actor: String, workspace: UUID, role: SupportRole)
    fun assignSupportRole(actor: String, workspace: UUID, subject: String, role: UUID?)
    fun pilotUsage(actor: String, workspace: UUID): PilotUsage
    fun confirmedCosts(actor: String, workspace: UUID): Map<String, Long?>
    fun operationsMetrics(actor: String, workspace: UUID): Map<String, Long?>
    fun recordConfirmedCost(actor: String, workspace: UUID, run: UUID, micros: Long, reference: String)
    fun analysisPaused(actor: String, workspace: UUID): Boolean
    fun setAnalysisPaused(actor: String, workspace: UUID, paused: Boolean)
    fun correctConfirmedCost(actor: String, workspace: UUID, run: UUID, correction: UUID,
        previous: Long, amount: Long, receipt: String)
    fun feedbackEnabled(actor: String, workspace: UUID): Boolean
    fun setFeedbackEnabled(actor: String, workspace: UUID, enabled: Boolean)
    fun submitFeedback(actor: String, workspace: UUID, run: UUID, rating: Int)
    fun withdrawFeedback(actor: String, workspace: UUID, run: UUID)
    fun feedbackRetention(actor: String, workspace: UUID): Int?
    fun setFeedbackRetention(actor: String, workspace: UUID, days: Int)
    fun purgeExpiredFeedback(actor: String, workspace: UUID): Int
    fun qualityQueue(actor: String, workspace: UUID): List<QualityFeedback>
    fun markFeedbackReviewed(actor: String, workspace: UUID, run: UUID)
    fun runHistory(actor: String, workspace: UUID): List<Run>
    fun incidents(actor: String, workspace: UUID): List<RunIncident>
    fun reviewIncident(actor: String, workspace: UUID, runId: UUID, disposition: IncidentDisposition)
    fun createDecision(subject: String, workspace: UUID, input: DecisionInput): Decision
    fun decisions(subject: String, workspace: UUID): List<Decision>
    fun decision(subject: String, workspace: UUID, id: UUID): Decision
    fun editDecision(subject: String, workspace: UUID, id: UUID, expectedRevision: Long, input: DecisionInput): Decision
    fun enqueue(subject: String, workspace: UUID, id: UUID, expectedRevision: Long, key: String): Run
    fun run(subject: String, workspace: UUID, id: UUID): Run
    fun cancel(subject: String, workspace: UUID, id: UUID): Run
    fun recoverExpired(): Int
    fun claim(leaseSeconds: Long = 120): Claim?
    fun complete(claim: Claim, completion: Completion)
    fun adminUsage(subject: String, workspace: UUID): Map<String, Long?>
}
