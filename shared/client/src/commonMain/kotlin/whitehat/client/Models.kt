package whitehat.client

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class Workspace(val id: String, val name: String, val role: String)

@Serializable
data class Profile(val subject: String, val workspaces: List<Workspace>)

@Serializable
data class DecisionInput(
    val title: String,
    val decision: String,
    val mode: String = "challenge",
    val objective: String = "",
    val context: String = "",
    @SerialName("source_notes") val sourceNotes: String = "",
)

@Serializable
data class Decision(
    val id: String,
    @SerialName("workspace_id") val workspaceId: String,
    val revision: Long,
    val title: String,
    val decision: String,
    val mode: String,
    val objective: String,
    val context: String,
    @SerialName("source_notes") val sourceNotes: String,
)

@Serializable
data class Decisions(val items: List<Decision>, val limit: Int)

@Serializable
data class DecisionMode(val id: String, val verdicts: List<String>)

@Serializable
data class Run(
    val id: String,
    @SerialName("workspace_id") val workspaceId: String,
    @SerialName("decision_id") val decisionId: String,
    @SerialName("input_revision") val inputRevision: Long,
    val status: String,
    @SerialName("output_contract") val outputContract: String,
    val result: JsonObject?,
    val error: String?,
    @SerialName("input_tokens") val inputTokens: Long?,
    @SerialName("output_tokens") val outputTokens: Long?,
) {
    val isActive: Boolean
        get() = status in setOf("queued", "running", "cancellation_requested")
}

/** Persist this intent before sending if a submission must survive an app restart. */
@Serializable
data class RunSubmission(
    val workspace: String,
    val decision: String,
    val revision: Long,
    val idempotencyKey: String,
) {
    init {
        require(revision > 0)
        require(idempotencyKey.matches(Regex("[A-Za-z0-9_-]{8,120}")))
    }
}
