package app.commutenity.domain

enum class Field { A, B }

enum class Sheet { Peek, Half, Notice }

data class Place(
    val id: String,
    val name: String,
    val area: String,
    val inMakati: Boolean,
)

data class Trip(
    val fare: String,
    val minutes: String,
    val transfers: String,
    val distanceLine: String,
    val reason: String,
    val sample: Boolean,
    val legs: List<Leg>,
)

sealed interface Leg {
    data class Walk(val meters: String) : Leg
    data class Ride(
        val stops: String,
        val fareAndMinutes: String,
        val signboard: String,
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
    fun resolve(origin: Place, destination: Place): TripResult
}
