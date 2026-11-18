package whitehat.adapters

import java.util.UUID
import whitehat.domain.Decision
import whitehat.domain.DecisionInput
import whitehat.domain.ProviderFailure

/** Staging-only admission. The reservation is never released after an ambiguous outcome. */
class G001Dispatch(
    private val workspace: UUID,
    private val operator: String,
    private val reserve: (String, UUID, String) -> Boolean,
) {
    companion object {
        const val BASELINE = "gpt-5.4-mini-2026-03-17"
        const val CANDIDATE = "gpt-5.4-2026-03-05"
        const val MAX_INPUT_TOKENS = 10_000L
        const val MAX_OUTPUT_TOKENS = 5_000
        internal val input = DecisionInput(
            "Golden evaluation G001",
            "I want to spend three months building an AI pricing assistant. Eight peers said they would try it for free. No one has paid.",
        )
    }

    fun admit(decision: Decision, model: String) {
        if (model !in setOf(BASELINE, CANDIDATE) || decision.workspace != workspace ||
            decision.owner != operator || decision.revision != 1L || decision.input != input) {
            throw ProviderFailure("G001_SCOPE_REJECTED")
        }
        if (!reserve(operator, workspace, model)) throw ProviderFailure("G001_ATTEMPT_USED")
    }
}
