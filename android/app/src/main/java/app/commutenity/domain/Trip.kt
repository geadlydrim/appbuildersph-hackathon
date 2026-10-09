package app.commutenity.domain

enum class Field { A, B }

enum class Sheet { Peek, Half, Notice }

data class Place(
    val id: String,
    val name: String,
    val area: String,
    val inMakati: Boolean,
    /** Map position. Null for places without coordinates (no pin is drawn for them). */
    val lat: Double? = null,
    val lng: Double? = null,
)

data class Trip(
    /** Stable candidate key; rider evidence ties to this, never to display text. */
    val key: String,
    val fare: String,
    val minutes: String,
    val transfers: String,
    val distanceLine: String,
    val reason: String,
    val sample: Boolean,
    val legs: List<Leg>,
    /** Where to draw the trip on the map. Null when the source has no coordinates (sample data). */
    val path: TripPath? = null,
)

data class GeoPoint(val lat: Double, val lng: Double)

/** Map geometry of a trip, in travel order. */
data class TripPath(
    /** One road-following line per ride leg. */
    val rides: List<List<GeoPoint>>,
    /** One line per walk (drawn dashed); a straight two-point line when no foot route is stored. */
    val walks: List<List<GeoPoint>>,
    /** Where the rider boards each ride. */
    val boardStops: List<GeoPoint>,
    /** Where the rider says "para" on the last ride. */
    val para: GeoPoint?,
)

sealed interface Leg {
    data class Walk(val meters: String) : Leg
    data class Ride(
        val stops: String,
        val fareAndMinutes: String,
        val signboard: String,
        /** Each signboard text this ride may show, from the pack. Feeds the correct-vehicle check. */
        val signboards: List<String> = emptyList(),
    ) : Leg
    data class Para(val landmark: String) : Leg
}

sealed interface TripResult {
    data class Ready(val trip: Trip) : TripResult
    data object NotInData : TripResult
}

sealed interface SearchRow {
    data object UseMyLocation : SearchRow
    data class PlaceRow(val place: Place) : SearchRow
}

/**
 * The only door the screens use for places and trips.
 * [app.commutenity.data.sample.SampleTripSource] is the current stand-in.
 */
interface TripSource {
    val myLocation: Place
    fun search(field: Field, query: String): List<SearchRow>
    fun resolve(origin: Place, destination: Place, preference: TripPreference = TripPreference.Default): TripResult
}

/** The rider's stated preference (from the ask bar); promotes one criterion in the D16 order. */
enum class TripPreference { Default, Fastest, Cheapest, FewestTransfers }
