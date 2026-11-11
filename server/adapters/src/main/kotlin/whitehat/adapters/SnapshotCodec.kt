package whitehat.adapters

import kotlinx.serialization.json.*
import java.util.UUID
import whitehat.contracts.Mode
import whitehat.domain.*

internal fun Decision.encode(): String = buildJsonObject {
    put("id", id.toString()); put("workspace", workspace.toString()); put("owner", owner); put("revision", revision)
    put("title", input.title); put("decision", input.decision); put("objective", input.objective)
    put("context", input.context); put("source_notes", input.sourceNotes); put("mode", input.mode.wireId)
}.toString()

internal fun decodeDecision(raw: String): Decision {
    val obj = Json.parseToJsonElement(raw).jsonObject
    fun str(key: String) = obj.getValue(key).jsonPrimitive.content
    return Decision(UUID.fromString(str("id")), UUID.fromString(str("workspace")), str("owner"), obj.getValue("revision").jsonPrimitive.long,
        DecisionInput(str("title"), str("decision"), str("objective"), str("context"), str("source_notes"), Mode.fromWire(str("mode"))!!))
}
