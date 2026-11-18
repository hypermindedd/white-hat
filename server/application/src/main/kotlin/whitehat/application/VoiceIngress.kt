package whitehat.application

import java.nio.charset.CharacterCodingException
import java.util.UUID
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull

enum class VoiceLeaseState { ACTIVE, PAUSED, CLOSED }

/** Supplied by the caller after authorization and a read of the current lease. */
data class VoiceCursor(
    val sessionId: UUID,
    val leaseId: UUID,
    val consumedThrough: Long,
    val state: VoiceLeaseState,
) {
    init { require(consumedThrough >= 0) }
}

sealed interface VoiceEffect {
    data object None : VoiceEffect
    data class FinalTranscript(val turnId: UUID, val text: String) : VoiceEffect
    data class InterruptPlayback(val turnId: UUID) : VoiceEffect
}

sealed interface VoiceIngressResult {
    data class Ready(val sequence: Long, val effect: VoiceEffect) : VoiceIngressResult
    data object Replay : VoiceIngressResult
    enum class Rejected : VoiceIngressResult { INVALID_FRAME, WRONG_LEASE, INACTIVE, SEQUENCE_GAP }
}

/**
 * Provider-neutral boundary for an already authorized voice lease.
 * Before ACK, the caller must compare the session, active lease and cursor again in one
 * transaction, persist any final turn once by turn ID, and advance the cursor.
 */
object VoiceIngress {
    private const val VERSION = "whitehat.voice.ingress.v1"
    private const val MAX_FRAME_BYTES = 16_384
    private const val MAX_TRANSCRIPT_LENGTH = 4_000
    private val frameKeys = setOf("version", "session_id", "lease_id", "sequence", "turn_id", "kind")

    fun accept(cursor: VoiceCursor, payload: ByteArray): VoiceIngressResult {
        if (cursor.state != VoiceLeaseState.ACTIVE) return VoiceIngressResult.Rejected.INACTIVE
        val frame = decode(payload) ?: return VoiceIngressResult.Rejected.INVALID_FRAME
        if (frame.sessionId != cursor.sessionId || frame.leaseId != cursor.leaseId) {
            return VoiceIngressResult.Rejected.WRONG_LEASE
        }
        if (frame.sequence <= cursor.consumedThrough) return VoiceIngressResult.Replay
        if (frame.sequence - 1 != cursor.consumedThrough) return VoiceIngressResult.Rejected.SEQUENCE_GAP
        return VoiceIngressResult.Ready(frame.sequence, frame.effect)
    }

    private data class Frame(val sessionId: UUID, val leaseId: UUID, val sequence: Long, val effect: VoiceEffect)

    private fun decode(payload: ByteArray): Frame? {
        if (payload.isEmpty() || payload.size > MAX_FRAME_BYTES) return null
        val json = try {
            Json.parseToJsonElement(payload.decodeToString(throwOnInvalidSequence = true)) as? JsonObject
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: CharacterCodingException) {
            null
        } ?: return null
        if (json.string("version") != VERSION) return null
        val session = json.uuid("session_id") ?: return null
        val lease = json.uuid("lease_id") ?: return null
        val turn = json.uuid("turn_id") ?: return null
        val sequence = (json["sequence"] as? JsonPrimitive)?.takeUnless { it.isString }
            ?.longOrNull?.takeIf { it > 0 } ?: return null
        val kind = json.string("kind")
        val fields = when (kind) {
            "transcript_interim", "transcript_final" -> frameKeys + "text"
            "playback_interrupted" -> frameKeys
            else -> return null
        }
        if (json.keys != fields) return null
        val effect = when (kind) {
            "playback_interrupted" -> VoiceEffect.InterruptPlayback(turn)
            else -> {
                val transcript = json.string("text")
                    ?.takeIf { it.length <= MAX_TRANSCRIPT_LENGTH && it.isNotBlank() } ?: return null
                if (kind == "transcript_final") VoiceEffect.FinalTranscript(turn, transcript) else VoiceEffect.None
            }
        }
        return Frame(session, lease, sequence, effect)
    }

    private fun JsonObject.string(key: String): String? =
        (get(key) as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun JsonObject.uuid(key: String): UUID? {
        val value = string(key) ?: return null
        return try {
            UUID.fromString(value).takeIf { it.toString() == value }
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
