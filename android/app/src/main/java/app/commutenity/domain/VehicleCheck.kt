package app.commutenity.domain

/**
 * "Is this the right jeep?" (D27). Deterministic: the rider's text (typed, or pulled out by the
 * on-device AI) is compared with the signboards stored in the pack for the next ride. The AI never
 * decides the verdict. It is conservative: a partial match is "not sure", never "yes" (AI-05 allows
 * zero false "Yes, ride this").
 */
sealed interface VehicleVerdict {
    /** Every word of one expected signboard was read. */
    data class RideThis(val matched: String) : VehicleVerdict

    /** Nothing the rider read appears on any expected signboard. */
    data class LookFor(val expected: List<String>) : VehicleVerdict

    /** Some words overlap, or the text is unreadable. */
    data class NotSure(val expected: List<String>) : VehicleVerdict
}

object VehicleCheck {
    fun check(read: String, signboards: List<String>): VehicleVerdict {
        val expected = signboards.filter { it.isNotBlank() }
        val readWords = words(read)
        if (readWords.isEmpty() || expected.isEmpty()) return VehicleVerdict.NotSure(expected)

        // Prefer the most specific signboard ("Buendia - LRT" over "LRT") when several match.
        val full = expected
            .filter { board -> words(board).let { it.isNotEmpty() && it.all { word -> readWords.any { r -> same(word, r) } } } }
            .maxByOrNull { words(it).size }
        if (full != null) return VehicleVerdict.RideThis(full)

        val anyOverlap = expected.any { board -> words(board).any { word -> readWords.any { r -> same(word, r) } } }
        return if (anyOverlap) VehicleVerdict.NotSure(expected) else VehicleVerdict.LookFor(expected)
    }

    /** What the rider sees. */
    fun message(read: String, verdict: VehicleVerdict): String {
        val said = "You read \"${read.trim()}\"."
        return when (verdict) {
            is VehicleVerdict.RideThis -> "$said Yes, ride this. It matches \"${verdict.matched}\"."
            is VehicleVerdict.LookFor -> "$said No, look for ${quoted(verdict.expected)}."
            is VehicleVerdict.NotSure ->
                if (verdict.expected.isEmpty()) "$said Not sure. Set A and B first so I know which ride you need."
                else "$said Not sure. Check the signboard for ${quoted(verdict.expected)}."
        }
    }

    private fun quoted(boards: List<String>) = boards.joinToString(" or ") { "\"$it\"" }

    private fun words(text: String): List<String> =
        text.lowercase().split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotEmpty() }

    /** Equal, or one typo apart for words of 5+ letters ("buendia" / "buendya"). */
    private fun same(a: String, b: String): Boolean =
        a == b || (a.length >= 5 && b.length >= 5 && editDistance(a, b) <= 1)

    private fun editDistance(a: String, b: String): Int {
        var previous = IntArray(b.length + 1) { it }
        for (i in a.indices) {
            val current = IntArray(b.length + 1)
            current[0] = i + 1
            for (j in b.indices) {
                current[j + 1] = minOf(previous[j + 1] + 1, current[j] + 1, previous[j] + if (a[i] == b[j]) 0 else 1)
            }
            previous = current
        }
        return previous[b.length]
    }
}
