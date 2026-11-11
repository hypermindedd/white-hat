package whitehat.contracts

import kotlinx.serialization.json.*

/** Migration boundary for alpha.1 output, NOT the canonical envelope 1.0.
 * No prompts, provider credentials or authority to execute a decision live here.
 * The server must validate again even when a client uses this validator.
 */
object LegacyOutputValidator {
    data class Violation(val path: String, val code: String)

    private sealed interface Shape
    private data class Text(val values: Set<String>? = null) : Shape
    private data class Record(val fields: Map<String, Shape>) : Shape
    private data class Items(val item: Shape) : Shape
    private val text = Text()
    private val confidences = setOf("LOW", "MODERATE", "HIGH", "VERY HIGH")

    private fun schema(mode: Mode) = Record(linkedMapOf(
        "mode" to Text(setOf(mode.wireId)),
        "decision_question" to text,
        "verdict" to Text(mode.verdicts + "NEEDS_CONTEXT"),
        "confidence" to Text(confidences),
        "summary" to text,
        "top_challenge" to text,
        "findings" to Items(Record(linkedMapOf(
            "kind" to Text(setOf("assumption", "blind_spot", "evidence", "trade_off", "consequence")),
            "title" to text, "explanation" to text,
            "evidence_status" to Text(setOf("provided", "inferred", "unknown"))
        ))),
        "strongest_counterargument" to text, "failure_scenario" to text,
        "critical_unknowns" to Items(Record(linkedMapOf(
            "question" to text, "decision_effect" to text, "resolve_by" to text
        ))),
        "change_conditions" to Items(text),
        "next_action" to Record(linkedMapOf(
            "action" to text, "purpose" to text, "observable_result" to text, "decision_effect" to text
        )),
        "clarification_question" to text
    ))

    fun validate(value: JsonElement, mode: Mode): List<Violation> {
        val errors = mutableListOf<Violation>()
        fun walk(element: JsonElement, shape: Shape, path: String) {
            fun fail(code: String) { errors += Violation(path, code) }
            when (shape) {
                is Record -> {
                    val obj = element as? JsonObject
                    if (obj == null) { fail("OBJECT_REQUIRED"); return }
                    for (key in shape.fields.keys - obj.keys) errors += Violation("$path.$key", "MISSING")
                    for (key in obj.keys - shape.fields.keys) errors += Violation(path, "UNEXPECTED_FIELD")
                    for ((key, child) in shape.fields) obj[key]?.let { walk(it, child, "$path.$key") }
                }
                is Items -> {
                    val array = element as? JsonArray
                    if (array == null) { fail("ARRAY_REQUIRED"); return }
                    if (array.size > 12) fail("TOO_MANY_ENTRIES")
                    array.forEachIndexed { index, child -> walk(child, shape.item, "$path.$index") }
                }
                is Text -> {
                    val primitive = element as? JsonPrimitive
                    if (primitive == null || !primitive.isString) fail("STRING_REQUIRED")
                    else if (primitive.content.length > 12000) fail("TOO_LONG")
                    else if (shape.values != null && primitive.content !in shape.values) fail("INVALID_ENUM")
                }
            }
        }
        walk(value, schema(mode), "result")
        if (errors.isNotEmpty()) return errors
        val obj = value.jsonObject
        fun string(key: String) = obj.getValue(key).jsonPrimitive.content
        if (string("decision_question").isBlank() || string("summary").isBlank()) {
            errors += Violation("result", "DECISION_AND_SUMMARY_REQUIRED")
        }
        if (string("verdict") == "NEEDS_CONTEXT") {
            if (string("clarification_question").isBlank()) {
                errors += Violation("result.clarification_question", "DECISIVE_QUESTION_REQUIRED")
            }
        } else {
            if (string("top_challenge").isBlank()) errors += Violation("result.top_challenge", "CHALLENGE_REQUIRED")
            for ((key, element) in obj.getValue("next_action").jsonObject) {
                if (element.jsonPrimitive.content.isBlank()) errors += Violation("result.next_action.$key", "ACTION_REQUIRED")
            }
        }
        if (obj.getValue("critical_unknowns").jsonArray.isNotEmpty() && string("confidence") in setOf("HIGH", "VERY HIGH")) {
            errors += Violation("result.confidence", "CRITICAL_UNKNOWN_CONFIDENCE_CEILING")
        }
        return errors
    }
}
