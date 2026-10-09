package app.commutenity.data.pack

import app.commutenity.domain.Place

/** Offline index of the places declared by a commute pack. */
class PackPlaceIndex(private val places: List<PackPlace>) {
    fun search(query: String): List<Place> {
        if (query.isBlank()) return places.map { it.place }
        val needle = key(query)
        // Only street words ("street", "st.") or punctuation: nothing to match on.
        if (needle.isEmpty()) return emptyList()

        return places.mapNotNull { packPlace ->
            val score = listOf(packPlace.place.name, *packPlace.aliases.toTypedArray())
                .map(::key)
                .filter { it.isNotEmpty() }
                .mapNotNull { candidate -> matchScore(needle, candidate) }
                .minOrNull()
            score?.let { it to packPlace.place }
        }.sortedWith(compareBy<Pair<Int, Place>> { it.first }.thenBy { it.second.name })
            .map { it.second }
    }

    private fun matchScore(needle: String, candidate: String): Int? {
        if (needle == candidate) return 0
        if (candidate.contains(needle) || needle.contains(candidate)) return 1
        val maximumDistance = when (needle.length) {
            in 0..4 -> 0
            in 5..7 -> 1
            else -> 2
        }
        val distance = editDistance(needle, candidate)
        return distance.takeIf { it <= maximumDistance }?.plus(2)
    }

    /**
     * Comparison key: lowercase letters and digits only, with street-type words dropped and no spaces,
     * so "delarosa street", "Dela Rosa St." and "dela rosa" all become "delarosa".
     */
    private fun key(value: String): String =
        value.lowercase()
            .split(Regex("[^\\p{L}\\p{N}]+"))
            .filter { it.isNotEmpty() && it !in STREET_WORDS }
            .joinToString("")

    private fun editDistance(left: String, right: String): Int {
        var previous = IntArray(right.length + 1) { it }
        left.forEachIndexed { leftIndex, leftCharacter ->
            val current = IntArray(right.length + 1)
            current[0] = leftIndex + 1
            right.forEachIndexed { rightIndex, rightCharacter ->
                current[rightIndex + 1] = minOf(
                    previous[rightIndex + 1] + 1,
                    current[rightIndex] + 1,
                    previous[rightIndex] + if (leftCharacter == rightCharacter) 0 else 1,
                )
            }
            previous = current
        }
        return previous[right.length]
    }
}

data class PackPlace(
    val place: Place,
    val aliases: List<String>,
)

/** Street-type words riders add, spell out, or abbreviate; they never tell two places apart. */
private val STREET_WORDS = setOf(
    "st", "street", "ave", "av", "avenue", "rd", "road", "blvd", "boulevard", "hwy", "highway",
)
