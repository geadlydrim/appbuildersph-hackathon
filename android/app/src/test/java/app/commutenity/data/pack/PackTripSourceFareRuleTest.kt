package app.commutenity.data.pack

import app.commutenity.domain.Field
import app.commutenity.domain.Leg
import app.commutenity.domain.Place
import app.commutenity.domain.SearchRow
import app.commutenity.domain.Trip
import app.commutenity.domain.TripAnswer
import app.commutenity.domain.TripResult
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Test

class PackTripSourceFareRuleTest {
    private val fixture = CommutePack.parse(
        checkNotNull(javaClass.classLoader?.getResource("carousel-fixture.json")) { "carousel-fixture.json missing" }.readText(),
    )
    private val fixtureSource = PackTripSource(fixture)

    private val hero = CommutePack.parse(File("../../data/pack/hero-trip.source.json").readText())
    private val rufino = hero.places.first { it.id == "va-rufino" }.toPlace()
    private val delaRosa = hero.places.first { it.id == "dela-rosa-pio-del-pilar" }.toPlace()

    private fun place(id: String): Place = fixture.places.first { it.id == id }.toPlace()

    private fun ready(result: TripResult): Trip = (result as TripResult.Ready).trip

    @Test
    fun fixtureParsesTheFareRuleAndPublicProvenance() {
        assertEquals(listOf("bus_aircon", "bus_aircon", null), fixture.routes.map { it.fareRule })
        assertTrue(hero.routes.all { it.fareRule == null })
        assertTrue(fixture.segments.all { it.sourceClass == CommutePack.SOURCE_PUBLIC })
        assertTrue(fixture.segments.filter { it.routeId != "fx-c" }.all { it.farePhp == null && it.minutes == null && it.minutesEst != null })
    }

    @Test
    fun twoSegmentsOfAFareRuleRouteAreOneRideWithOneFare() {
        val trip = ready(fixtureSource.resolve(place("fx-pa1"), place("fx-pa3")))
        val ride = trip.legs.filterIsInstance<Leg.Ride>().single()

        assertEquals(listOf(Leg.Ride::class, Leg.Para::class), trip.legs.map { it::class })
        assertEquals("Fixture A Stop 1  →  Fixture A Stop 3", ride.stops)
        // 3.0 km is inside the first 5 km: the 18 minimum once, not 18 + 18.
        assertEquals("₱18", ride.fare)
        assertEquals("~26 min (est.)", ride.minutes)
        assertEquals("₱18  ·  ~26 min (est.)", ride.fareAndMinutes)
        assertEquals("₱18", trip.fare)
        assertEquals("~26 min (est.)", trip.minutes)
        assertEquals("0", trip.transfers)
        assertFalse(trip.unverified)
        assertEquals(1, trip.path!!.rides.size)
    }

    @Test
    fun aRideOverFiveKilometresAddsTheSucceedingKilometres() {
        val trip = ready(fixtureSource.resolve(place("fx-pb1"), place("fx-pb3")))
        val ride = trip.legs.filterIsInstance<Leg.Ride>().single()

        // 6.2 km: 18 + 2 succeeding km x 2.98 = 23.96, which rounds to 24.
        assertEquals("₱24", ride.fare)
        assertEquals("₱24", trip.fare)
        assertEquals("~52 min (est.)", trip.minutes)
        assertFalse(trip.unverified)
    }

    @Test
    fun aRouteWithoutAFareRuleStillSumsItsSegmentFares() {
        val trip = ready(fixtureSource.resolve(place("fx-pc1"), place("fx-pc3")))

        assertEquals("₱17", trip.fare)
        assertEquals("₱17", trip.legs.filterIsInstance<Leg.Ride>().single().fare)
        assertFalse(trip.unverified)
    }

    @Test
    fun aPublicTripCarriesNoOsmNote() {
        val trip = ready(fixtureSource.resolve(place("fx-pa1"), place("fx-pa3")))
        val answer = TripAnswer.compose(trip)

        assertFalse(answer, answer.contains(TripAnswer.OSM_NOTE_ENGLISH))
        assertTrue(answer, answer.contains("₱18"))
    }

    @Test
    fun theHeroGraphLeavesOutPublicSegmentsPlacedOnTheHeroStops() {
        val publicFast = hero.copy(
            routes = hero.routes + PackRoute("pub-fast", "Public fast", "bus", listOf("PUB"), sourceClass = CommutePack.SOURCE_PUBLIC),
            segments = hero.segments + PackSegment(
                routeId = "pub-fast",
                seq = 1,
                fromStopId = "gil-puyat-h267",
                toStopId = "gil-puyat-osmena",
                farePhp = 5,
                minutes = null,
                minutesEst = 1,
                distanceM = 848,
                polyline = null,
                sourceClass = CommutePack.SOURCE_PUBLIC,
            ),
        )
        val source = PackTripSource(publicFast)

        val heroKeys = source.candidates(rufino, delaRosa).map { it.key }
        assertEquals(listOf("jeep-buendia-lrt#1", "bus-buendia-lrt#1"), heroKeys)

        // Control: any other place on the same stops does see the public ride, and it is not marked unverified.
        val from = Place("copy-from", "Copy of Rufino", "", inMakati = true, lat = rufino.lat, lng = rufino.lng)
        val to = Place("copy-to", "Copy of Dela Rosa", "", inMakati = true, lat = delaRosa.lat, lng = delaRosa.lng)
        val publicTrip = source.candidates(from, to).first { it.key.startsWith("pub-fast") }
        assertFalse(publicTrip.unverified)
        assertEquals("₱5", publicTrip.fare)
    }

    @Test
    fun theRealMergedPacksPriceTheCarouselPerRide() {
        val carouselFile = File("../../data/pack/carousel-makati.source.json")
        Assume.assumeTrue(carouselFile.exists())
        val osmFile = File("../../data/pack/osm-makati.source.json")
        Assume.assumeTrue(osmFile.exists())

        val merged = hero
            .merge(CommutePack.parse(osmFile.readText()))
            .merge(CommutePack.parse(carouselFile.readText()))
        val source = PackTripSource(merged)

        fun find(name: String): Place = source.search(Field.B, name)
            .filterIsInstance<SearchRow.PlaceRow>()
            .map { it.place }
            .first { it.name == name }

        val guadalupe = find("EDSA Carousel Guadalupe")
        val ayala = find("EDSA Carousel Ayala")
        val trip = ready(source.resolve(guadalupe, ayala))
        val rides = trip.legs.filterIsInstance<Leg.Ride>()

        assertEquals(1, rides.size)
        assertTrue(trip.key, trip.key.startsWith("carousel-sb#1"))
        assertEquals("₱18", rides.single().fare)
        assertEquals("₱18", trip.fare)
        assertEquals("0", trip.transfers)
        assertFalse(trip.unverified)
        assertTrue(rides.single().signboards.contains("EDSA Carousel"))

        // The hero trip is unchanged by the extra packs.
        assertEquals("₱14", ready(source.resolve(rufino, delaRosa)).fare)
    }

    @Test
    fun anUnknownFareRuleGivesAnUnknownFare() {
        val pack = fixture.copy(routes = fixture.routes.map { if (it.id == "fx-a") it.copy(fareRule = "tricycle") else it })
        val trip = ready(PackTripSource(pack).resolve(place("fx-pa1"), place("fx-pa3")))

        assertEquals("Fare unknown", trip.fare)
        assertEquals("Fare unknown", trip.legs.filterIsInstance<Leg.Ride>().single().fare)
        assertNull(FareRules.fare("tricycle", 3000))
    }
}
