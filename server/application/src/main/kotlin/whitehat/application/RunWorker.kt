package whitehat.application

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import whitehat.contracts.LegacyOutputValidator
import whitehat.domain.*

class RunWorker(private val store: DecisionStore, private val provider: DecisionProvider) {
    suspend fun tick(): Boolean {
        withContext(Dispatchers.IO) { store.recoverExpired() }
        if (!provider.configured) return false
        val claim = withContext(Dispatchers.IO) { store.claim() } ?: return false
        val completion = try {
            val response = provider.analyze(claim.snapshot)
            val valid = runCatching { LegacyOutputValidator.validate(Json.parseToJsonElement(response.output), claim.snapshot.input.mode).isEmpty() }.getOrDefault(false)
            if (valid) Completion(output = response.output, receipt = response.receipt)
            else Completion(error = "INVALID_MODEL_OUTPUT", receipt = response.receipt)
        } catch (cancelled: CancellationException) {
            // A stopped worker cannot prove a provider request was not charged.
            // Leave the lease for conservative reconciliation; do not retry.
            throw cancelled
        } catch (failure: ProviderFailure) {
            Completion(error = failure.code, uncertain = failure.uncertain, receipt = failure.receipt)
        } catch (_: Exception) {
            Completion(error = "PROVIDER_OUTCOME_UNKNOWN", uncertain = true)
        }
        withContext(Dispatchers.IO) { store.complete(claim, completion) }
        return true
    }
}
