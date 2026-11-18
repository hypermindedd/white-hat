package whitehat.api

import java.net.URI
import java.net.http.HttpClient
import java.net.http.WebSocket
import java.time.Duration
import java.util.Base64
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import whitehat.domain.Fault

internal class OpenAiLiveTranscription(private val apiKey: String) {
    private val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()

    fun open(): TranscriptionTurn {
        val queue = ArrayBlockingQueue<JsonObject>(256)
        val listener = object : WebSocket.Listener {
            private val fragments = StringBuilder()
            override fun onText(socket: WebSocket, data: CharSequence, last: Boolean): java.util.concurrent.CompletionStage<*>? {
                if (fragments.length + data.length > 32_768) {
                    socket.abort()
                    return null
                }
                fragments.append(data)
                if (last) {
                    val event = runCatching { Json.parseToJsonElement(fragments.toString()) as? JsonObject }.getOrNull()
                    fragments.clear()
                    if (event == null || !queue.offer(event)) {
                        socket.abort()
                        return null
                    }
                }
                socket.request(1)
                return null
            }
        }
        val connection = client.newWebSocketBuilder()
            .header("Authorization", "Bearer " + apiKey)
            .connectTimeout(Duration.ofSeconds(5))
            .buildAsync(URI("wss://api.openai.com/v1/realtime?intent=transcription"), listener)
        val socket = try { connection.get(5, TimeUnit.SECONDS) }
        catch (_: Exception) {
            connection.thenAccept { it.abort() }
            throw Fault(503, "VOICE_PROVIDER_UNAVAILABLE")
        }
        return try {
            val turn = OpenTurn(socket, queue)
            turn.configure()
            turn
        } catch (failure: Exception) {
            socket.abort()
            throw failure
        }
    }

    private class OpenTurn(private val socket: WebSocket, private val events: ArrayBlockingQueue<JsonObject>) : TranscriptionTurn {
        fun configure() {
            send("""{"type":"session.update","session":{"type":"transcription","audio":{"input":{"format":{"type":"audio/pcm","rate":24000},"transcription":{"model":"gpt-live-transcribe"},"turn_detection":null}}}}""")
            until(4) { if (it.text("type") == "session.updated") "ready" else null }
        }

        override fun append(pcm: ByteArray) {
            send(buildJsonObject {
                put("type", "input_audio_buffer.append")
                put("audio", Base64.getEncoder().encodeToString(pcm))
            }.toString())
        }

        override fun finish(): String {
            send("""{"type":"input_audio_buffer.commit"}""")
            var committed: String? = null
            return until(8) { event ->
                val type = event.text("type")
                val id = event.text("item_id")
                when {
                    type == "input_audio_buffer.committed" -> { committed = id; null }
                    committed != null && id == committed && type == "conversation.item.input_audio_transcription.completed" ->
                        event.text("transcript") ?: throw Fault(502, "VOICE_TRANSCRIPT_INVALID")
                    committed != null && id == committed && type == "conversation.item.input_audio_transcription.failed" ->
                        throw Fault(502, "VOICE_TRANSCRIPTION_FAILED")
                    else -> null
                }
            }
        }

        private fun until(seconds: Long, match: (JsonObject) -> String?): String {
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds)
            while (true) {
                val remaining = deadline - System.nanoTime()
                if (remaining <= 0) throw Fault(504, "VOICE_PROVIDER_TIMEOUT")
                val event = events.poll(remaining, TimeUnit.NANOSECONDS) ?: throw Fault(504, "VOICE_PROVIDER_TIMEOUT")
                if (event.text("type") == "error") throw Fault(502, "VOICE_PROVIDER_REJECTED")
                match(event)?.let { return it }
            }
        }

        private fun send(frame: String) {
            try { socket.sendText(frame, true).get(3, TimeUnit.SECONDS) }
            catch (_: Exception) { throw Fault(503, "VOICE_PROVIDER_UNAVAILABLE") }
        }

        override fun close() { socket.abort() }

        private fun JsonObject.text(key: String): String? =
            (get(key) as? JsonPrimitive)?.takeIf { it.isString }?.content
    }
}
