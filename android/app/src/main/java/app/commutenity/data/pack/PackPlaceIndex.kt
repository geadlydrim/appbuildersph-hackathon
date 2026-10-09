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
            score?.let { it to packPlace }
        }.sortedWith(
            // Best match first; on a tie, the team's own places before imported ones (an OSM bus stop
            // named "Dela Rosa" must not displace the hero destination), then the shorter name.
            compareBy<Pair<Int, PackPlace>> { it.first }
                .thenBy { !it.second.preferred }
                .thenBy { it.second.place.name.length }
                .thenBy { it.second.place.name },
        ).map { it.second.place }
    }

    /** 0 exact; 1 name starts with the query; 2 name contains it; 3 a short name inside the query; then typos. */
    private fun matchScore(needle: String, candidate: String): Int? {
        if (needle == candidate) return 0
        if (candidate.startsWith(needle)) return 1
        if (candidate.contains(needle)) return 2
        if (needle.contains(candidate)) return 3
        val maximumDistance = when (needle.length) {
            in 0..4 -> 0
            in 5..7 -> 1
            else -> 2
        }
        val distance = editDistance(needle, candidate)
        return distance.takeIf { it <= maximumDistance }?.plus(4)
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
    /** Team data (`known`/`collected`); wins ties against imported (OSM) places. */
    val preferred: Boolean = false,
)

/** Street-type words riders add, spell out, or abbreviate; they never tell two places apart. */
private val STREET_WORDS = setOf(
    "st", "street", "ave", "av", "avenue", "rd", "road", "blvd", "boulevard", "hwy", "highway",
)
