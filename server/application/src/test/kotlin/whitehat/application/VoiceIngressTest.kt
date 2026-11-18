package whitehat.application

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class VoiceIngressTest {
    private val session = UUID.fromString("11111111-1111-4111-8111-111111111111")
    private val lease = UUID.fromString("22222222-2222-4222-8222-222222222222")
    private val turn = UUID.fromString("33333333-3333-4333-8333-333333333333")
    private val initial = VoiceCursor(session, lease, 0, VoiceLeaseState.ACTIVE)

    @Test fun replaysOrderedSyntheticFramesAndForwardsOnlyFinalText() {
        var cursor = initial
        val intermediate = frame(1, "transcript_interim", "قیمت")
        val final = frame(2, "transcript_final", "قیمت نهایی ۱۲۰ یورو است.")
        val interrupt = frame(3, "playback_interrupted")
        val outcomes = listOf(intermediate, final, final, interrupt).map { payload ->
            VoiceIngress.accept(cursor, payload).also {
                if (it is VoiceIngressResult.Ready) cursor = cursor.copy(consumedThrough = it.sequence)
            }
        }
        assertEquals(listOf(
            VoiceIngressResult.Ready(1, VoiceEffect.None),
            VoiceIngressResult.Ready(2, VoiceEffect.FinalTranscript(turn, "قیمت نهایی ۱۲۰ یورو است.")),
            VoiceIngressResult.Replay,
            VoiceIngressResult.Ready(3, VoiceEffect.InterruptPlayback(turn)),
        ), outcomes)
        assertEquals(3L, cursor.consumedThrough)
    }

    @Test fun gapDoesNotAdvanceAndNewLeaseRejectsOldFrames() {
        val later = frame(2, "transcript_final", "second")
        assertEquals(VoiceIngressResult.Rejected.SEQUENCE_GAP, VoiceIngress.accept(initial, later))
        val first = assertIs<VoiceIngressResult.Ready>(VoiceIngress.accept(initial, frame(1, "transcript_final", "first")))
        val next = initial.copy(consumedThrough = first.sequence)
        assertEquals(VoiceIngressResult.Ready(2, VoiceEffect.FinalTranscript(turn, "second")), VoiceIngress.accept(next, later))
        val renewed = UUID.fromString("44444444-4444-4444-8444-444444444444")
        val resumed = next.copy(leaseId = renewed, consumedThrough = 0)
        assertEquals(VoiceIngressResult.Rejected.WRONG_LEASE, VoiceIngress.accept(resumed, later))
        assertEquals(VoiceIngressResult.Ready(1, VoiceEffect.FinalTranscript(turn, "new")), VoiceIngress.accept(
            resumed, frame(1, "transcript_final", "new", targetLease = renewed),
        ))
        assertEquals(VoiceIngressResult.Rejected.WRONG_LEASE, VoiceIngress.accept(
            initial, frame(1, "transcript_final", "wrong session", targetSession = renewed),
        ))
    }

    @Test fun pausedOrClosedCursorCannotBeReopenedByAnEvent() {
        val event = frame(1, "transcript_final", "new")
        for (state in listOf(VoiceLeaseState.PAUSED, VoiceLeaseState.CLOSED)) {
            assertEquals(VoiceIngressResult.Rejected.INACTIVE, VoiceIngress.accept(initial.copy(state = state), event))
        }
    }

    @Test fun malformedOrUnboundedFramesAreRejected() {
        val valid = frame(1, "transcript_final", "text").decodeToString()
        val bad = listOf(
            "{".encodeToByteArray(),
            byteArrayOf(0xc3.toByte(), 0x28),
            ByteArray(16_385),
            valid.replace("\"sequence\":1", "\"sequence\":\"1\"").encodeToByteArray(),
            valid.replace("\"sequence\":1", "\"sequence\":0").encodeToByteArray(),
            valid.replace("ingress.v1", "ingress.v2").encodeToByteArray(),
            valid.dropLast(1).plus(",\"mode\":\"override\"}").encodeToByteArray(),
            frame(1, "transcript_final", " ".repeat(5)),
            frame(1, "transcript_final", "x".repeat(4001)),
            valid.replace("\"text\":\"text\"", "\"text\":\"\\u0000\"").encodeToByteArray(),
            valid.replace("\"text\":\"text\"", "\"text\":\"\\uD800\"").encodeToByteArray(),
            frame(1, "playback_interrupted", "not allowed"),
            frame(1, "provider_error"),
        )
        for ((index, payload) in bad.withIndex()) {
            assertEquals(VoiceIngressResult.Rejected.INVALID_FRAME, VoiceIngress.accept(initial, payload), "fixture ${index + 1}")
        }
    }

    private fun frame(
        sequence: Long, kind: String, text: String? = null,
        targetLease: UUID = lease, targetSession: UUID = session,
    ): ByteArray = buildJsonObject {
        put("version", "whitehat.voice.ingress.v1")
        put("session_id", targetSession.toString())
        put("lease_id", targetLease.toString())
        put("sequence", sequence)
        put("turn_id", turn.toString())
        put("kind", kind)
        if (text != null) put("text", text)
    }.toString().encodeToByteArray()
}
