package app.commutenity.ai

/**
 * Hybrid parser: code classifies (vehicle check, preference), the LLM only copies spans out of the
 * question, and code verifies every copied span before it can reach place search.
 */
object HybridParser {
    private val NON_ALNUM = Regex("[^\\p{L}\\p{N}]+")

    fun isVehicleQuestion(question: String): Boolean = ParserCues.VEHICLE.containsMatchIn(question)

    fun preferenceOf(question: String): Preference? =
        ParserCues.PREFERENCE.firstOrNull { it.second.containsMatchIn(question) }?.first

    /** `["vehicle_text"]` for vehicle questions, else `["origin", "destination"]`. */
    fun fieldsFor(question: String): List<String> =
        if (isVehicleQuestion(question)) listOf("vehicle_text") else listOf("origin", "destination")

    // Enum-of-spans constraints failed on device (the decoder closed every string after its first word),
    // so the schema allows free strings and `copied` checks the copy.
    fun schemaFor(fields: List<String>): String {
        val properties = fields.joinToString(",") { "\"$it\":{\"type\":[\"string\",\"null\"]}" }
        val required = fields.joinToString(",") { "\"$it\"" }
        return "{\"type\":\"object\",\"properties\":{$properties},\"required\":[$required],\"additionalProperties\":false}"
    }

    private fun norm(s: String): String = s.lowercase().replace(NON_ALNUM, " ").trim()

    /**
     * Keeps an extracted value only if the rider actually wrote it: edge punctuation and a "pa-" prefix are
     * stripped, then the value must appear word-aligned in the question. Anything else becomes null, so an
     * invented place can never reach place search.
     */
    fun copied(value: String?, question: String): String? {
        if (value == null) return null
        var v = value.trim { !it.isLetterOrDigit() }
        if (v.length > 3 && v.startsWith("pa-", ignoreCase = true)) v = v.substring(3)
        val nv = norm(v)
        if (nv.isEmpty()) return null
        return v.takeIf { " ${norm(question)} ".contains(" $nv ") }
    }

    fun assemble(question: String, extracted: Map<String, String?>): ParserOutput {
        fun field(key: String): String? = copied(extracted[key], question)

        if (isVehicleQuestion(question)) {
            return ParserOutput(Intent.VEHICLE_CHECK, null, null, null, field("vehicle_text"))
        }
        val destination = field("destination")
        // A trip cannot start where it ends; the model echoes the one place it found into both fields.
        val origin = field("origin")?.takeUnless { destination != null && norm(it) == norm(destination) }
        val isTrip = origin != null || destination != null
        return ParserOutput(
            intent = if (isTrip) Intent.TRIP else Intent.OTHER,
            origin = origin,
            destination = destination,
            preference = if (isTrip) preferenceOf(question) else null,
            vehicleText = null,
        )
    }
}
