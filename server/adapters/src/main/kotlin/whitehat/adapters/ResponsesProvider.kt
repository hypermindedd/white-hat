package whitehat.adapters

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.*
import java.security.MessageDigest
import whitehat.domain.*

class EnginePolicy {
    private val raw = requireNotNull(javaClass.getResourceAsStream("/engine/policies.json")).use { it.readBytes() }
    val hash: String = MessageDigest.getInstance("SHA-256").digest(raw).joinToString("") { "%02x".format(it) }
    private val modes = Json.parseToJsonElement(raw.toString(Charsets.UTF_8)).jsonObject.getValue("modes").jsonObject
    fun forMode(mode: String): JsonObject = modes.getValue(mode).jsonObject
}

class ResponsesProvider(private val apiKey: String, private val model: String, private val policy: EnginePolicy,
    private val client: HttpClient = defaultClient(), private val g001: G001Dispatch? = null) : DecisionProvider, AutoCloseable {
    override val configured: Boolean get() = apiKey.isNotBlank() && model.isNotBlank()
    companion object {
        fun defaultClient() = HttpClient(CIO) {
            followRedirects = false
            install(HttpTimeout) { requestTimeoutMillis = 60000; connectTimeoutMillis = 10000; socketTimeoutMillis = 60000 }
        }
    }
    override suspend fun analyze(decision: Decision): ProviderResponse {
        if (!configured) throw ProviderFailure("PROVIDER_NOT_CONFIGURED")
        val mode = policy.forMode(decision.input.mode.wireId)
        val input = buildJsonObject {
            put("decision", decision.input.decision); put("objective", decision.input.objective)
            put("context", decision.input.context); put("source_notes", decision.input.sourceNotes)
            put("active_mode", decision.input.mode.wireId)
        }
        val request = buildJsonObject {
            put("model", model); put("store", false); put("max_output_tokens", G001Dispatch.MAX_OUTPUT_TOKENS)
            put("instructions", mode.getValue("instructions")); put("input", input.toString())
            if (g001 != null) put("service_tier", "default")
            putJsonObject("text") { putJsonObject("format") {
                put("type", "json_schema"); put("name", "second_mind_decision"); put("strict", true); put("schema", mode.getValue("schema"))
            } }
        }
        try {
            if (g001 != null) {
                g001.admit(decision, model)
                preflightInputTokens(request)
            }
            val response = client.post("https://api.openai.com/v1/responses") {
                bearerAuth(apiKey); contentType(ContentType.Application.Json); setBody(request.toString())
            }
            if (response.status.value !in 200..299) {
                throw ProviderFailure(when (response.status.value) { 401, 403 -> "PROVIDER_AUTH_FAILED"; 429 -> "PROVIDER_RATE_LIMIT"; else -> "PROVIDER_UNAVAILABLE" }, response.status.value >= 500)
            }
            val raw = response.bodyAsText()
            if (raw.length > 2_000_000) throw ProviderFailure("INVALID_PROVIDER_RESPONSE", true)
            val body = Json.parseToJsonElement(raw).jsonObject
            val usage = body["usage"] as? JsonObject
            fun count(key: String): Long? = usage?.get(key)?.jsonPrimitive?.longOrNull?.takeIf { it >= 0 }
            val receipt = Receipt(body["model"]?.jsonPrimitive?.contentOrNull ?: model, body["id"]?.jsonPrimitive?.contentOrNull, count("input_tokens"), count("output_tokens"))
            if (body["status"]?.jsonPrimitive?.content != "completed") throw ProviderFailure("MODEL_RESPONSE_INCOMPLETE", receipt = receipt)
            val content = body["output"]?.jsonArray.orEmpty().map { it.jsonObject }.filter { it["type"]?.jsonPrimitive?.content == "message" }
                .flatMap { it["content"]?.jsonArray.orEmpty() }.map { it.jsonObject }
            if (content.any { it["type"]?.jsonPrimitive?.content == "refusal" }) throw ProviderFailure("MODEL_REFUSED", receipt = receipt)
            val output = content.filter { it["type"]?.jsonPrimitive?.content == "output_text" }.joinToString("") { it.getValue("text").jsonPrimitive.content }
            return ProviderResponse(output, receipt)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: ProviderFailure) { throw failure }
        catch (_: Exception) { throw ProviderFailure("PROVIDER_OUTCOME_UNKNOWN", true) }
    }
    private suspend fun preflightInputTokens(request: JsonObject) {
        val countRequest = JsonObject(request.filterKeys { it in setOf("model", "instructions", "input", "text") })
        val count = try {
            val response = client.post("https://api.openai.com/v1/responses/input_tokens") {
                bearerAuth(apiKey)
                contentType(ContentType.Application.Json)
                setBody(countRequest.toString())
            }
            if (!response.status.isSuccess()) throw ProviderFailure("G001_TOKEN_PRECHECK_FAILED")
            val raw = response.bodyAsText()
            if (raw.length > 4096) throw ProviderFailure("G001_TOKEN_PRECHECK_FAILED")
            Json.parseToJsonElement(raw).jsonObject["input_tokens"]?.jsonPrimitive?.longOrNull
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            throw ProviderFailure("G001_TOKEN_PRECHECK_FAILED")
        }
        if (count == null || count < 1) throw ProviderFailure("G001_TOKEN_PRECHECK_FAILED")
        if (count > G001Dispatch.MAX_INPUT_TOKENS) throw ProviderFailure("G001_INPUT_TOKEN_LIMIT")
    }

    override fun close() { client.close() }
}
