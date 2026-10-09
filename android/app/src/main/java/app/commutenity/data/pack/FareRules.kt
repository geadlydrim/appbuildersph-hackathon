package app.commutenity.data.pack

/**
 * The owner's fare table (supplied 2026-10-10): a minimum fare covering the first kilometres, plus a per-kilometre
 * rate for each succeeding kilometre (a started kilometre counts in full).
 */
object FareRules {
    private class Rule(val baseCentavos: Int, val baseMeters: Int, val perKmCentavos: Int)

    private val rules = mapOf(
        "bus_aircon" to Rule(baseCentavos = 1800, baseMeters = 5000, perKmCentavos = 298),
        "bus_ordinary" to Rule(baseCentavos = 1500, baseMeters = 5000, perKmCentavos = 249),
        "jeep_traditional" to Rule(baseCentavos = 1400, baseMeters = 4000, perKmCentavos = 200),
        "jeep_modern" to Rule(baseCentavos = 1700, baseMeters = 4000, perKmCentavos = 240),
    )

    /** Whole-peso fare (nearest peso, half up) for a ride of [distanceM] metres under [rule]; null for an unknown rule. */
    fun fare(rule: String, distanceM: Int): Int? {
        val entry = rules[rule] ?: return null
        val extraKm = if (distanceM > entry.baseMeters) (distanceM - entry.baseMeters + 999) / 1000 else 0
        val centavos = entry.baseCentavos + extraKm * entry.perKmCentavos
        return (centavos + 50) / 100
    }
}
