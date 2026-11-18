package whitehat.domain

import java.util.UUID

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
