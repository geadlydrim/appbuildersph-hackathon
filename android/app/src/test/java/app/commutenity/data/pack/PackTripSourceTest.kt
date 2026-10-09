package app.commutenity.data.pack

import app.commutenity.domain.Field
import app.commutenity.domain.GeoPoint
import app.commutenity.domain.Leg
import app.commutenity.domain.Place
import app.commutenity.domain.SearchRow
import app.commutenity.domain.Trip
import app.commutenity.domain.TripPreference
import app.commutenity.domain.TripResult
import java.io.File
import kotlin.math.cos
import kotlin.math.hypot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PackTripSourceTest {
    private val pack = CommutePack.parse(File("../../data/pack/hero-trip.source.json").readText())
    private val source = PackTripSource(pack)

    private val rufino = pack.places.first { it.id == "va-rufino" }.toPlace()
    private val delaRosa = pack.places.first { it.id == "dela-rosa-pio-del-pilar" }.toPlace()

    private fun names(query: String, field: Field = Field.B): List<String> =
        source.search(field, query).filterIsInstance<SearchRow.PlaceRow>().map { it.place.name }

    private fun ready(result: TripResult): Trip = (result as TripResult.Ready).trip

    private fun metresBetween(a: GeoPoint, lat: Double, lng: Double): Double =
        hypot((a.lat - lat) * 111_195.0, (a.lng - lng) * 111_195.0 * cos(Math.toRadians(lat)))

    @Test
    fun findsVaRufinoByNameAliasAndTypo() {
        for (query in listOf("rufino", "ruffino", "VA Rufino")) {
            assertEquals(query, listOf("V.A. Rufino St"), names(query))
        }
    }

    @Test
    fun findsDelaRosaByAliasAndTypo() {
        for (query in listOf("dela rosa", "delarosa", "Pio del Pilar")) {
            assertEquals(query, listOf("Dela Rosa St, Pio del Pilar"), names(query))
        }
    }

    @Test
    fun unknownPlaceFindsNothing() {
        assertTrue(names("tofino").isEmpty())
    }

    @Test
    fun fieldAListsMyLocationFirstAndFieldBDoesNot() {
        assertEquals(SearchRow.UseMyLocation, source.search(Field.A, "").first())
        assertEquals(2, names("", Field.A).size)
        assertTrue(source.search(Field.B, "").none { it is SearchRow.UseMyLocation })
    }

    @Test
    fun myLocationIsAnHonestDemoStandIn() {
        assertEquals("My location (demo: V.A. Rufino St)", source.myLocation.name)
        assertEquals(rufino.lat, source.myLocation.lat)
        assertEquals(rufino.lng, source.myLocation.lng)
    }

    @Test
    fun resolvesHeroTripWithCheapestJeepFirst() {
        val trip = ready(source.resolve(rufino, delaRosa))

        assertEquals("₱12", trip.fare)
        assertEquals("Cheapest", trip.reason)
        assertEquals("0", trip.transfers)
        assertEquals(false, trip.sample)
        assertEquals(listOf(Leg.Walk::class, Leg.Ride::class, Leg.Walk::class, Leg.Para::class), trip.legs.map { it::class })

        val ride = trip.legs[1] as Leg.Ride
        assertTrue(ride.signboard.contains("LRT"))
        assertEquals("Gil Puyat Ave (San Antonio)  →  Gil Puyat Ave near Osmeña Hwy / PNR", ride.stops)
        assertEquals("₱12  ·  7 min", ride.fareAndMinutes)
        assertEquals("~540 m", (trip.legs[0] as Leg.Walk).meters)
        assertEquals("Gil Puyat Ave near Osmeña Hwy / PNR", (trip.legs[3] as Leg.Para).landmark)
        assertTrue(trip.key.startsWith("jeep-buendia-lrt#1"))
    }

    @Test
    fun tripPathFollowsTheStoredShape() {
        val trip = ready(source.resolve(rufino, delaRosa))
        val path = checkNotNull(trip.path)
        val board = pack.stops.first { it.id == "gil-puyat-h267" }

        assertEquals(1, path.rides.size)
        assertTrue(path.rides[0].size >= 3)
        assertTrue(metresBetween(path.rides[0].first(), board.lat, board.lng) < 30.0)
        assertEquals(2, path.walks.size)
        assertEquals(GeoPoint(board.lat, board.lng), path.boardStops.single())
        val alight = pack.stops.first { it.id == "gil-puyat-osmena" }
        assertEquals(GeoPoint(alight.lat, alight.lng), path.para)
    }

    @Test
    fun cheapestPreferenceStillPicksTheJeep() {
        val trip = ready(source.resolve(rufino, delaRosa, TripPreference.Cheapest))
        assertEquals("₱12", trip.fare)
        assertEquals("Cheapest", trip.reason)
        assertTrue(trip.key.startsWith("jeep-buendia-lrt"))
    }

    @Test
    fun candidatesListsJeepThenBus() {
        val trips = source.candidates(rufino, delaRosa)
        assertEquals(listOf("jeep-buendia-lrt#1", "bus-buendia-lrt#1"), trips.map { it.key })
        assertEquals(ready(source.resolve(rufino, delaRosa)), trips.first())
    }

    @Test
    fun netVotesOnlyBreakAnExactTieSoTheCheaperJeepStaysFirst() {
        val voted = PackTripSource(pack, netVotes = { if (it.startsWith("bus")) 3 else 0 })
        assertEquals(listOf("jeep-buendia-lrt#1", "bus-buendia-lrt#1"), voted.candidates(rufino, delaRosa).map { it.key })
    }

    @Test
    fun noCandidatesWhenThereIsNoRouteData() {
        assertTrue(source.candidates(delaRosa, rufino).isEmpty())
    }

    @Test
    fun pinnedSpotNearVaRufinoResolvesViaNearestStop() {
        val pin = Place("pin", "Pinned spot", "", inMakati = true, lat = 14.5585, lng = 121.0180)
        val trip = ready(source.resolve(pin, delaRosa))

        assertEquals("₱12", trip.fare)
        val walk = trip.legs.first() as Leg.Walk
        assertTrue(walk.meters, walk.meters.startsWith("~4"))
    }

    @Test
    fun pinnedSpotFarFromEveryStopIsNotInData() {
        val pin = Place("pin", "Pinned spot", "", inMakati = true, lat = 14.53, lng = 121.05)
        assertEquals(TripResult.NotInData, source.resolve(pin, delaRosa))
        assertEquals(TripResult.NotInData, source.resolve(rufino, pin))
    }

    @Test
    fun outsideMakatiIsNotInData() {
        val outside = Place("outside", "Quezon City", "", inMakati = false, lat = 14.676, lng = 121.044)
        assertEquals(TripResult.NotInData, source.resolve(outside, delaRosa))
        assertEquals(TripResult.NotInData, source.resolve(rufino, outside))
    }

    @Test
    fun decodesKnownPolyline() {
        // Google's documented example, precision 5.
        val points = Polyline.decode("_p~iF~ps|U_ulLnnqC_mqNvxq`@", precision = 5)

        assertEquals(
            listOf(GeoPoint(38.5, -120.2), GeoPoint(40.7, -120.95), GeoPoint(43.252, -126.453)),
            points,
        )
    }
}
