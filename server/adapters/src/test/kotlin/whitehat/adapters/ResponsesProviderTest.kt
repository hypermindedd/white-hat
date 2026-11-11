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
}
