package app.commutenity.ai

enum class Intent(val wire: String) {
    TRIP("trip"),
    VEHICLE_CHECK("vehicle_check"),
    OTHER("other"),
}

enum class Preference(val wire: String) {
    CHEAPEST("cheapest"),
    FASTEST("fastest"),
    FEWEST_TRANSFERS("fewest_transfers"),
}

data class ParserOutput(
    val intent: Intent,
    val origin: String?,
    val destination: String?,
    val preference: Preference?,
    val vehicleText: String?,
) {
    /**
     * SDD §4 parser JSON: `{"intent":..,"origin":..,"destination":..,"preference":..,"vehicle_text":..}`,
     * nulls as JSON null. Hand-built so it runs in plain JVM tests (no org.json).
     */
    fun toJson(): String = buildString {
        append("{\"intent\":").append(jsonString(intent.wire))
        append(",\"origin\":").append(jsonString(origin))
        append(",\"destination\":").append(jsonString(destination))
        append(",\"preference\":").append(jsonString(preference?.wire))
        append(",\"vehicle_text\":").append(jsonString(vehicleText))
        append('}')
    }
}

private fun jsonString(value: String?): String {
    if (value == null) return "null"
    val sb = StringBuilder(value.length + 2).append('"')
    for (c in value) {
        when (c) {
            '"' -> sb.append("\\\"")
            '\\' -> sb.append("\\\\")
            '\n' -> sb.append("\\n")
            '\r' -> sb.append("\\r")
            '\t' -> sb.append("\\t")
            else -> if (c < ' ') sb.append("\\u%04x".format(c.code)) else sb.append(c)
        }
    }
    return sb.append('"').toString()
}
