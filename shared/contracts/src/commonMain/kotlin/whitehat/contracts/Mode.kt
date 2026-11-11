package whitehat.contracts

/** Stable IDs and verdict vocabularies from the recovered alpha engine. */
enum class Mode(val wireId: String, val verdicts: Set<String>) {
    CHALLENGE("challenge", commonVerdicts()),
    KILL("kill", setOf("BUILD", "VALIDATE FIRST", "PIVOT", "KILL")),
    BLIND("blind", commonVerdicts()),
    PREMORTEM("premortem", commonVerdicts()),
    REALITY("reality", setOf("DO IT", "CHANGE IT", "TEST IT", "DON'T DO IT")),
    COMPARE("compare", setOf("RECOMMENDATION", "CONDITIONAL RECOMMENDATION")),
    DEVIL("devil", setOf("YES", "PARTIALLY", "NO"));

    companion object {
        fun fromWire(value: String): Mode? = entries.firstOrNull { it.wireId == value }
    }
}

private fun commonVerdicts() = setOf("PROCEED", "PROCEED WITH CONDITIONS", "TEST FIRST", "RETHINK", "STOP")
