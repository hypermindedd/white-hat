package whitehat.adapters

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.http.content.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import java.util.UUID
import kotlin.test.*
import whitehat.contracts.Mode
import whitehat.domain.*

class ResponsesProviderTest {
    private fun decision() = Decision(UUID.randomUUID(), UUID.randomUUID(), "fixture-subject", 1, DecisionInput("پایلوت", "پایلوت اجرا شود؟", "سنجش پرداخت", mode = Mode.CHALLENGE))
    @Test fun requestsUsePinnedPolicyStrictSchemaAndActualUsage() = runBlocking {
        var calls = 0
        val client = HttpClient(MockEngine { request ->
            calls++
            assertEquals("https://api.openai.com/v1/responses", request.url.toString())
            val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
            assertFalse(body.getValue("store").jsonPrimitive.boolean)
            assertEquals("json_schema", body.getValue("text").jsonObject.getValue("format").jsonObject.getValue("type").jsonPrimitive.content)
            assertTrue(body.getValue("text").jsonObject.getValue("format").jsonObject.getValue("strict").jsonPrimitive.boolean)
            assertFalse(body.toString().contains("fixture-key-not-real"))
            respond("""{"status":"completed","id":"fixture-response","model":"fixture-model","usage":{"input_tokens":12,"output_tokens":7},"output":[{"type":"message","content":[{"type":"output_text","text":"{}"}]}]}""", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType,"application/json"))
        })
        ResponsesProvider("fixture-key-not-real", "fixture-model", EnginePolicy(), client).use { provider ->
            val result = provider.analyze(decision())
            assertEquals("{}", result.output); assertEquals(12L, result.receipt.inputTokens); assertEquals(1, calls)
        }
    }
    @Test fun missingConfigNeverCallsProvider() = runBlocking {
        var calls = 0
        val client = HttpClient(MockEngine { calls++; error("must not execute") })
        ResponsesProvider("", "", EnginePolicy(), client).use { provider ->
            assertEquals("PROVIDER_NOT_CONFIGURED", assertFailsWith<ProviderFailure> { provider.analyze(decision()) }.code)
        }
        assertEquals(0, calls)
    }
    @Test fun refusalRetainsActualReceiptWithoutAValidJudgment() = runBlocking {
        val client = HttpClient(MockEngine {
            respond("""{"status":"completed","id":"fixture-refusal","model":"fixture-model","usage":{"input_tokens":3,"output_tokens":2},"output":[{"type":"message","content":[{"type":"refusal","refusal":"fixture-only"}]}]}""", HttpStatusCode.OK)
        })
        ResponsesProvider("fixture-key", "fixture-model", EnginePolicy(), client).use { provider ->
            val failure = assertFailsWith<ProviderFailure> { provider.analyze(decision()) }
            assertEquals("MODEL_REFUSED", failure.code); assertEquals(3L, failure.receipt?.inputTokens); assertFalse(failure.uncertain)
        }
    }
    @Test fun remoteErrorsAreRedactedAndNotRetried() = runBlocking {
        var calls = 0
        val client = HttpClient(MockEngine { calls++; respond("remote-private-diagnostic", HttpStatusCode.InternalServerError) })
        ResponsesProvider("fixture-key", "fixture-model", EnginePolicy(), client).use { provider ->
            val failure = assertFailsWith<ProviderFailure> { provider.analyze(decision()) }
            assertEquals("PROVIDER_UNAVAILABLE", failure.code); assertTrue(failure.uncertain)
            assertFalse(failure.message.orEmpty().contains("remote-private"))
        }
        assertEquals(1, calls)
    }
    @Test fun malformedSuccessfulResponseRequiresReconciliation() = runBlocking {
        val client = HttpClient(MockEngine { respond("not-json", HttpStatusCode.OK) })
        ResponsesProvider("fixture-key", "fixture-model", EnginePolicy(), client).use { provider ->
            val failure = assertFailsWith<ProviderFailure> { provider.analyze(decision()) }
            assertEquals("PROVIDER_OUTCOME_UNKNOWN", failure.code); assertTrue(failure.uncertain)
        }
    }
    @Test fun g001RejectsWrongInputAndCannotGenerateOverTheInputLimit() = runBlocking {
        var reserved = false
        var calls = 0
        val expected = decision().copy(input = G001Dispatch.input)
        val guard = G001Dispatch(expected.workspace, expected.owner) { _, _, _ ->
            (!reserved).also { reserved = true }
        }
        val client = HttpClient(MockEngine { request ->
            calls++
            assertEquals("/v1/responses/input_tokens", request.url.encodedPath)
            val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
            assertEquals(setOf("model", "instructions", "input", "text"), body.keys)
            respond("""{"object":"response.input_tokens","input_tokens":10001}""", HttpStatusCode.OK)
        })
        ResponsesProvider("fixture-key", G001Dispatch.BASELINE, EnginePolicy(), client, guard).use { provider ->
            assertEquals("G001_SCOPE_REJECTED", assertFailsWith<ProviderFailure> { provider.analyze(decision()) }.code)
            assertEquals(0, calls)
            assertEquals("G001_INPUT_TOKEN_LIMIT", assertFailsWith<ProviderFailure> { provider.analyze(expected) }.code)
            assertEquals("G001_ATTEMPT_USED", assertFailsWith<ProviderFailure> { provider.analyze(expected) }.code)
            assertEquals(1, calls)
        }
    }

    @Test fun g001CountsSameInputsAndMakesExactlyOneBoundedGeneration() = runBlocking {
        val expected = decision().copy(input = G001Dispatch.input)
        val paths = mutableListOf<String>()
        var counted: JsonObject? = null
        val client = HttpClient(MockEngine { request ->
            paths += request.url.encodedPath
            val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
            if (request.url.encodedPath.endsWith("/input_tokens")) {
                counted = body
                respond("""{"object":"response.input_tokens","input_tokens":10000}""", HttpStatusCode.OK)
            } else {
                assertEquals(counted, JsonObject(body.filterKeys { it in requireNotNull(counted).keys }))
                assertEquals(5000, body.getValue("max_output_tokens").jsonPrimitive.int)
                assertEquals("default", body.getValue("service_tier").jsonPrimitive.content)
                respond("""{"status":"completed","id":"fixture-only","model":"gpt-5.4-2026-03-05","usage":{"input_tokens":10000,"output_tokens":24},"output":[{"type":"message","content":[{"type":"output_text","text":"{}"}]}]}""",
                    HttpStatusCode.OK)
            }
        })
        val guard = G001Dispatch(expected.workspace, expected.owner) { _, _, _ -> true }
        ResponsesProvider("fixture-key", G001Dispatch.CANDIDATE, EnginePolicy(), client, guard).use { provider ->
            assertEquals("fixture-only", provider.analyze(expected).receipt.providerId)
        }
        assertEquals(listOf("/v1/responses/input_tokens", "/v1/responses"), paths)
    }

}
