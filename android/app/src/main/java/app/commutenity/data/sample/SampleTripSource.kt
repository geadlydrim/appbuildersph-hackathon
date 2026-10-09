package app.commutenity.data.sample

import app.commutenity.domain.Field
import app.commutenity.domain.Leg
import app.commutenity.domain.Place
import app.commutenity.domain.SearchRow
import app.commutenity.domain.Trip
import app.commutenity.domain.TripResult
import app.commutenity.domain.TripSource

/**
 * Placeholder pack for the T0 screens. Pair names are the hero pair.
 * Leg values are the TBD tokens and are marked sample in the UI.
 * Nothing here is a real route, fare, or minute.
 */
class SampleTripSource : TripSource {
    private val ayala = Place(
        id = "ayala-center",
        name = "Ayala Center",
        area = "Station Rd, San Lorenzo",
        inMakati = true,
    )
    private val delaRosa = Place(
        id = "dela-rosa",
        name = "Dela Rosa St",
        area = "Pio del Pilar",
        inMakati = true,
    )
    private val outside = Place(
        id = "outside-makati",
        name = "Outside Makati",
        area = "",
        inMakati = false,
    )

    private val places = listOf(ayala, delaRosa, outside)

    override val myLocation: Place = ayala

    override fun search(field: Field, query: String): List<SearchRow> {
        val needle = query.trim().lowercase()
        val matches = places.filter { place ->
            needle.isEmpty() ||
                place.name.lowercase().contains(needle) ||
                place.area.lowercase().contains(needle)
        }
        return buildList {
            if (field == Field.A) add(SearchRow.UseMyLocation)
            matches.forEach { add(SearchRow.PlaceRow(it)) }
        }
    }

    override fun resolve(origin: Place, destination: Place): TripResult {
        if (!origin.inMakati || !destination.inMakati) return TripResult.NotInData
        return TripResult.Ready(
            Trip(
                fare = "₱XX",
                minutes = "XX min",
                transfers = "X",
                distanceLine = "X.X km   ·   walk X min",
                reason = "Fewest transfers",
                sample = true,
                legs = listOf(
                    Leg.Walk("~XX m"),
                    Leg.Ride(
                        stops = "Stop 1  →  Stop 2",
                        fareAndMinutes = "₱XX  ·  XX min",
                        signboard = "SIGNBOARD (TBD)",
                    ),
                    Leg.Walk("~XX m"),
                    Leg.Para("Landmark (TBD)"),
                ),
            ),
        )
    }
}
