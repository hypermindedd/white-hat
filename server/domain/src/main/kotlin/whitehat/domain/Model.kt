package whitehat.domain

import java.util.UUID
import whitehat.contracts.Mode

class Fault(val status: Int, val code: String) : RuntimeException(code)
enum class Role { OWNER, ADMIN, MEMBER, SUPPORT }
data class Workspace(val id: UUID, val name: String, val role: Role)
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
