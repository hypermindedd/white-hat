package whitehat.contracts

import kotlinx.serialization.json.*
import kotlin.test.*

class LegacyOutputValidatorTest {
    private val valid = Json.parseToJsonElement("""
        {"mode":"challenge","decision_question":"آیا پایلوت اجرا شود؟","verdict":"TEST FIRST",
        "confidence":"MODERATE","summary":"پرداخت هنوز سنجیده نشده.","top_challenge":"نبود شاهد مستقل",
        "findings":[{"kind":"assumption","title":"تقاضا","explanation":"فرض پرداخت","evidence_status":"provided"}],
        "strongest_counterargument":"تعهد منابع پیش از آزمون","failure_scenario":"عدم پرداخت",
        "critical_unknowns":[{"question":"پرداخت؟","decision_effect":"مقدار تعهد","resolve_by":"پایلوت پولی"}],
        "change_conditions":["ثبت پرداخت"],"next_action":{"action":"پایلوت پیشنهاد کن","purpose":"سنجش تقاضا",
        "observable_result":"پذیرش یا رد ثبت‌شده","decision_effect":"بازنگری پیشنهاد در صورت رد"},"clarification_question":""}
    """).jsonObject

    private fun replace(key: String, value: JsonElement) = JsonObject(valid + (key to value))

    @Test fun validLocalizedOutputIsAccepted() {
        assertTrue(LegacyOutputValidator.validate(valid, Mode.CHALLENGE).isEmpty())
    }

    @Test fun allSevenModesPreserveTheirWireIdsAndVerdicts() {
        assertEquals(7, Mode.entries.size)
        for (mode in Mode.entries) {
            assertEquals(mode, Mode.fromWire(mode.wireId))
            val sample = JsonObject(valid + mapOf("mode" to JsonPrimitive(mode.wireId), "verdict" to JsonPrimitive(mode.verdicts.first())))
            assertTrue(LegacyOutputValidator.validate(sample, mode).isEmpty(), mode.wireId)
        }
        assertNull(Mode.fromWire("negotiation")) // Interaction channel, not an eighth mode.
    }

    @Test fun criticalUnknownsCannotAccompanyHighConfidence() {
        for (band in listOf("HIGH", "VERY HIGH")) {
            assertTrue(LegacyOutputValidator.validate(replace("confidence", JsonPrimitive(band)), Mode.CHALLENGE)
                .any { it.code == "CRITICAL_UNKNOWN_CONFIDENCE_CEILING" })
        }
    }

    @Test fun blockedJudgmentRequiresAQuestionAndNoInventedAction() {
        val blocked = JsonObject(valid + mapOf("verdict" to JsonPrimitive("NEEDS_CONTEXT"),
            "top_challenge" to JsonPrimitive(""), "clarification_question" to JsonPrimitive("هدف تصمیم چیست؟")))
        assertTrue(LegacyOutputValidator.validate(blocked, Mode.CHALLENGE).isEmpty())
        assertTrue(LegacyOutputValidator.validate(JsonObject(blocked + ("clarification_question" to JsonPrimitive(""))), Mode.CHALLENGE).isNotEmpty())
    }

    @Test fun missingFieldsUnknownFieldsAndNonStringValuesAreRejected() {
        assertTrue(LegacyOutputValidator.validate(JsonObject(valid - "summary"), Mode.CHALLENGE).any { it.code == "MISSING" })
        assertTrue(LegacyOutputValidator.validate(replace("confidence", JsonPrimitive(0.99)), Mode.CHALLENGE).any { it.code == "STRING_REQUIRED" })
        assertTrue(LegacyOutputValidator.validate(replace("provider_key", JsonPrimitive("secret")), Mode.CHALLENGE).any { it.code == "UNEXPECTED_FIELD" })
        assertTrue(LegacyOutputValidator.validate(JsonNull, Mode.CHALLENGE).isNotEmpty())
    }

    @Test fun modeVocabularyCannotLeakAcrossModes() {
        assertTrue(LegacyOutputValidator.validate(replace("verdict", JsonPrimitive("BUILD")), Mode.CHALLENGE).any { it.code == "INVALID_ENUM" })
    }

    @Test fun incompleteActionAndUnboundedCollectionsAreRejected() {
        val action = valid.getValue("next_action").jsonObject
        val incomplete = replace("next_action", JsonObject(action + ("observable_result" to JsonPrimitive(" "))))
        assertTrue(LegacyOutputValidator.validate(incomplete, Mode.CHALLENGE).any { it.code == "ACTION_REQUIRED" })
        val tooMany = replace("change_conditions", JsonArray(List(13) { JsonPrimitive("x") }))
        assertTrue(LegacyOutputValidator.validate(tooMany, Mode.CHALLENGE).any { it.code == "TOO_MANY_ENTRIES" })
        assertTrue(LegacyOutputValidator.validate(replace("summary", JsonPrimitive("x".repeat(12001))), Mode.CHALLENGE).any { it.code == "TOO_LONG" })
    }
}
