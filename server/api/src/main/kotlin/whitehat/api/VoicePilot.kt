package whitehat.api

import java.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import whitehat.adapters.PostgresStore
import whitehat.application.VoiceCursor
import whitehat.application.VoiceEffect
import whitehat.application.VoiceIngress
import whitehat.application.VoiceIngressResult
import whitehat.application.VoiceLeaseState
import whitehat.domain.Fault

internal interface TranscriptionTurn : AutoCloseable {
    fun append(pcm: ByteArray)
    fun finish(): String
}

internal class VoicePilot(
    private val store: PostgresStore,
    private val workspace: UUID,
    private val openTurn: () -> TranscriptionTurn,
) : AutoCloseable {
    companion object {
        const val MAX_AUDIO_BYTES = 24_000 * 2 * 15
        const val MAX_CHUNK_BYTES = 24_000
        private const val LEASE_SECONDS = 40L
    }

    private class Lease(val actor: String, val turn: TranscriptionTurn, var bytes: Int = 0)
    private val leases = ConcurrentHashMap<UUID, Lease>()
    private val timer = Executors.newSingleThreadScheduledExecutor {
        Thread(it, "whitehat-voice-pilot-expiry").apply { isDaemon = true }
    }

    fun check(actor: String, requestedWorkspace: UUID) {
        if (requestedWorkspace != workspace) throw Fault(404, "VOICE_PILOT_UNAVAILABLE")
        store.requireVoicePilotOwner(actor, workspace)
    }

    fun start(actor: String, requestedWorkspace: UUID): UUID {
        if (requestedWorkspace != workspace) throw Fault(404, "VOICE_PILOT_UNAVAILABLE")
        store.reserveVoicePilot(actor, workspace)
        val id = UUID.randomUUID()
        val lease = Lease(actor, openTurn())
        leases[id] = lease
        timer.schedule({
            leases.remove(id, lease)
            lease.turn.close()
        }, LEASE_SECONDS, TimeUnit.SECONDS)
        return id
    }

    fun append(actor: String, requestedWorkspace: UUID, id: UUID, encoded: String) {
        val lease = owned(actor, requestedWorkspace, id)
        if (encoded.length > MAX_CHUNK_BYTES * 4 / 3) throw Fault(413, "VOICE_CHUNK_TOO_LARGE")
        val pcm = try { Base64.getDecoder().decode(encoded) }
        catch (_: IllegalArgumentException) { throw Fault(400, "INVALID_AUDIO") }
        if (pcm.isEmpty() || pcm.size % 2 != 0 || pcm.size > MAX_CHUNK_BYTES) throw Fault(400, "INVALID_AUDIO")
        synchronized(lease) {
            if (leases[id] !== lease) throw Fault(404, "VOICE_LEASE_UNAVAILABLE")
            if (lease.bytes + pcm.size > MAX_AUDIO_BYTES) {
                leases.remove(id, lease)
                lease.turn.close()
                throw Fault(413, "VOICE_DURATION_LIMIT")
            }
            lease.bytes += pcm.size
            try { lease.turn.append(pcm) }
            catch (failure: Exception) {
                leases.remove(id, lease)
                lease.turn.close()
                throw failure
            }
        }
    }

    fun finish(actor: String, requestedWorkspace: UUID, id: UUID): String {
        val lease = owned(actor, requestedWorkspace, id)
        return synchronized(lease) {
            if (!leases.remove(id, lease)) throw Fault(404, "VOICE_LEASE_UNAVAILABLE")
            lease.turn.use { turn ->
                if (lease.bytes < 4_800) throw Fault(400, "VOICE_AUDIO_TOO_SHORT")
                val frame = buildJsonObject {
                    put("version", "whitehat.voice.ingress.v1")
                    put("session_id", id.toString())
                    put("lease_id", id.toString())
                    put("sequence", 1)
                    put("turn_id", id.toString())
                    put("kind", "transcript_final")
                    put("text", turn.finish())
                }.toString().encodeToByteArray()
                val accepted = VoiceIngress.accept(VoiceCursor(id, id, 0, VoiceLeaseState.ACTIVE), frame)
                ((accepted as? VoiceIngressResult.Ready)?.effect as? VoiceEffect.FinalTranscript)?.text
                    ?: throw Fault(502, "VOICE_TRANSCRIPT_INVALID")
            }
        }
    }

    fun cancel(actor: String, requestedWorkspace: UUID, id: UUID) {
        val lease = owned(actor, requestedWorkspace, id)
        if (leases.remove(id, lease)) lease.turn.close()
    }

    private fun owned(actor: String, requestedWorkspace: UUID, id: UUID): Lease {
        check(actor, requestedWorkspace)
        return leases[id]?.takeIf { it.actor == actor } ?: throw Fault(404, "VOICE_LEASE_UNAVAILABLE")
    }

    override fun close() {
        timer.shutdownNow()
        leases.values.forEach { it.turn.close() }
        leases.clear()
    }
}
