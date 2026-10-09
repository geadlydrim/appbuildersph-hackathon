package app.commutenity.data.pack

import app.commutenity.domain.Leg
import app.commutenity.domain.Place
import app.commutenity.domain.Trip
import app.commutenity.domain.TripAnswer
import app.commutenity.domain.TripPreference
import app.commutenity.domain.TripResult
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PackTripSourceOsmTest {
    private val hero = CommutePack.parse(File("../../data/pack/hero-trip.source.json").readText())
    private val osm = CommutePack.parse(
        checkNotNull(javaClass.classLoader?.getResource("osm-fixture.json")) { "osm-fixture.json missing" }.readText(),
    )
    private val merged = hero.merge(osm)

    private val heroSource = PackTripSource(merged)
    private val osmSource = PackTripSource(osm)

    private fun place(pack: CommutePack, id: String): Place = pack.places.first { it.id == id }.toPlace()

    private val rufino = place(hero, "va-rufino")
    private val delaRosa = place(hero, "dela-rosa-pio-del-pilar")

    private fun ready(result: TripResult): Trip = (result as TripResult.Ready).trip

    @Test
    fun heroFileStillParsesWithTheNewOptionalFields() {
        assertTrue(hero.segments.isNotEmpty())
        assertTrue(hero.segments.all { it.sourceClass == CommutePack.SOURCE_KNOWN && it.minutesEst == null })
        assertTrue(hero.routes.all { it.sourceClass == CommutePack.SOURCE_KNOWN })
        assertTrue(hero.places.all { it.sourceClass == CommutePack.SOURCE_KNOWN })
    }

    @Test
    fun osmFixtureParsesItsProvenanceAndEstimates() {
        assertTrue(osm.segments.all { it.sourceClass == CommutePack.SOURCE_OSM && it.minutes == null && it.farePhp == null })
        assertTrue(osm.segments.all { it.minutesEst != null })
        assertTrue(osm.routes.all { it.sourceClass == CommutePack.SOURCE_OSM })
        assertTrue(osm.places.all { it.sourceClass == CommutePack.SOURCE_OSM })
    }

    @Test
    fun mergingConcatenatesBothPacksWithTheHeroPlaceFirst() {
        assertEquals(hero.segments.size + osm.segments.size, merged.segments.size)
        assertEquals(hero.stops.size + osm.stops.size, merged.stops.size)
        assertEquals("My location (demo: V.A. Rufino St)", heroSource.myLocation.name)
    }

    @Test
    fun heroTripIsUnchangedByTheMergedOsmData() {
        val trip = ready(heroSource.resolve(rufino, delaRosa))
        val alone = ready(PackTripSource(hero).resolve(rufino, delaRosa))

        assertEquals(alone, trip)
        assertEquals("₱14", trip.fare)
        assertEquals("Cheapest", trip.reason)
        assertEquals("0", trip.transfers)
        assertFalse(trip.unverified)
        assertEquals("₱14  ·  7 min", (trip.legs[1] as Leg.Ride).fareAndMinutes)
        assertFalse(trip.minutes.contains("est."))

        val keys = heroSource.candidates(rufino, delaRosa).map { it.key }
        assertEquals(listOf("jeep-buendia-lrt#1", "bus-buendia-lrt#1"), keys)
        assertTrue(keys.none { it.contains("osm-") })
    }

    @Test
    fun theSameStopsForAnyOtherPlaceDoUseTheOsmRide() {
        // Control for the hero rule: osm-r9 overlays the hero stops and is faster, so without the rule it would win.
        val from = Place("copy-from", "Copy of Rufino", "", inMakati = true, lat = rufino.lat, lng = rufino.lng)
        val to = Place("copy-to", "Copy of Dela Rosa", "", inMakati = true, lat = delaRosa.lat, lng = delaRosa.lng)
        val trips = heroSource.candidates(from, to)

        assertTrue(trips.first().key.startsWith("osm-r9#1"))
        assertTrue(trips.first().unverified)
        assertTrue(trips.any { it.key.startsWith("jeep-buendia-lrt") })
    }

    @Test
    fun threeConsecutiveSegmentsOfOneRouteAreOneRide() {
        val trip = ready(osmSource.resolve(place(osm, "osm-p1"), place(osm, "osm-p4")))
        val ride = trip.legs.filterIsInstance<Leg.Ride>().single()

        assertEquals(listOf(Leg.Ride::class, Leg.Para::class), trip.legs.map { it::class })
        assertEquals("Fixture Stop 1", ride.board)
        assertEquals("Fixture Stop 4", ride.alight)
        assertEquals("Fixture Stop 1  →  Fixture Stop 4", ride.stops)
        assertEquals("Fare unknown", ride.fare)
        assertEquals("~15 min (est.)", ride.minutes)
        assertEquals("Fare unknown  ·  ~15 min (est.)", ride.fareAndMinutes)
        assertEquals(listOf("1", "Fixture East"), ride.signboards)
        assertEquals("bus", ride.mode)

        assertEquals("Fare unknown", trip.fare)
        assertEquals("~15 min (est.)", trip.minutes)
        assertEquals("0", trip.transfers)
        assertTrue(trip.unverified)
        // One drawn line per ride, boarding only once.
        assertEquals(1, trip.path!!.rides.size)
        assertEquals(1, trip.path!!.boardStops.size)
    }

    @Test
    fun aTransferThroughTheSharedStopRidesTwoRoutes() {
        val trip = ready(osmSource.resolve(place(osm, "osm-p1"), place(osm, "osm-p6")))

        assertEquals(listOf(Leg.Ride::class, Leg.Ride::class, Leg.Para::class), trip.legs.map { it::class })
        assertEquals("1", trip.transfers)
        val rides = trip.legs.filterIsInstance<Leg.Ride>()
        assertEquals("Fixture Stop 3 (shared)", rides[0].alight)
        assertEquals("Fixture Stop 3 (shared)", rides[1].board)
        assertEquals("~10 min (est.)", rides[0].minutes)
        assertEquals("~10 min (est.)", rides[1].minutes)
        assertEquals("~20 min (est.)", trip.minutes)
    }

    @Test
    fun aTransferThroughAShortWalkIsRideWalkRide() {
        val trip = ready(osmSource.resolve(place(osm, "osm-p1"), place(osm, "osm-p8")))

        assertEquals(listOf(Leg.Ride::class, Leg.Walk::class, Leg.Ride::class, Leg.Para::class), trip.legs.map { it::class })
        assertEquals("1", trip.transfers)
        val first = trip.legs[0] as Leg.Ride
        val walk = trip.legs[1] as Leg.Walk
        val second = trip.legs[2] as Leg.Ride
        assertEquals("Fixture Stop 4", first.alight)
        assertEquals("~150 m", walk.meters)
        assertEquals("2 min", walk.minutes)
        assertEquals("Fixture Stop 7 (150 m from stop 4)", second.board)
        assertEquals("~15 min (est.)", first.minutes)
        assertEquals("~5 min (est.)", second.minutes)
        // 15 + 2 on foot + 5, and the walk is part of the walking total.
        assertEquals("~22 min (est.)", trip.minutes)
        assertTrue(trip.distanceLine, trip.distanceLine.endsWith("walk 2 min"))
        assertTrue(trip.unverified)
        assertEquals(1, trip.path!!.walks.size)
    }

    @Test
    fun walkingLinksAreNeverTheFirstOrLastStepOfATrip() {
        // Stop 7 is 150 m from stop 4, but a link is not a ride: nothing rides 4 -> 7, so only the walk-only trip is left.
        assertEquals(listOf("walk"), osmSource.candidates(place(osm, "osm-p4"), place(osm, "osm-p7")).map { it.key })
        assertEquals(listOf("walk"), osmSource.candidates(place(osm, "osm-p7"), place(osm, "osm-p4")).map { it.key })
    }

    @Test
    fun theUnverifiedAnswerSaysSoWithoutNewNumbers() {
        val trip = ready(osmSource.resolve(place(osm, "osm-p1"), place(osm, "osm-p8")))
        val english = TripAnswer.compose(trip)
        val taglish = TripAnswer.compose(trip, taglish = true)

        assertTrue(english, english.endsWith("Fare unknown, ~22 min (est.) in all. " + TripAnswer.OSM_NOTE_ENGLISH))
        assertTrue(taglish, taglish.endsWith(TripAnswer.OSM_NOTE_TAGLISH))
        assertTrue(english, english.contains("Walk ~150 m to Fixture Stop 7 (150 m from stop 4)."))

        val tripNumbers = buildList<String> {
            add(trip.fare)
            add(trip.minutes)
            trip.legs.forEach { leg ->
                when (leg) {
                    is Leg.Walk -> { add(leg.meters); add(leg.minutes) }
                    is Leg.Ride -> { add(leg.stops); add(leg.fare); add(leg.minutes); add(leg.board); add(leg.alight); add(leg.signboard) }
                    is Leg.Para -> add(leg.landmark)
                }
            }
        }.flatMap { Regex("\\d+").findAll(it).map { m -> m.value }.toList() }.toSet()
        for (answer in listOf(english, taglish)) {
            Regex("\\d+").findAll(answer).map { it.value }.forEach { assertTrue("$it is not in the trip's fields", it in tripNumbers) }
        }
    }

    @Test
    fun cachedCandidatesAreReRankedWhenTheVotesChange() {
        // Two routes tie on every criterion; only the key (then votes) orders them.
        val pack = CommutePack(
            shapePrecision = 6,
            places = listOf(
                PackPlaceRecord("p1", "P1", "", emptyList(), 14.55, 121.0, listOf("s1")),
                PackPlaceRecord("p2", "P2", "", emptyList(), 14.57, 121.0, listOf("s2")),
            ),
            stops = listOf(PackStop("s1", "S1", 14.55, 121.0), PackStop("s2", "S2", 14.57, 121.0)),
            routes = listOf(PackRoute("ra", "A", "bus", listOf("A")), PackRoute("rb", "B", "bus", listOf("B"))),
            segments = listOf(
                PackSegment("ra", 1, "s1", "s2", farePhp = null, minutes = null, minutesEst = 5, distanceM = 600, polyline = null, sourceClass = "osm"),
                PackSegment("rb", 1, "s1", "s2", farePhp = null, minutes = null, minutesEst = 5, distanceM = 600, polyline = null, sourceClass = "osm"),
            ),
        )
        val boosted = hashSetOf<String>()
        val source = PackTripSource(pack, netVotes = { if (it in boosted) 2 else 0 })
        val from = pack.places[0].toPlace()
        val to = pack.places[1].toPlace()

        assertEquals(listOf("ra#1", "rb#1"), source.candidates(from, to).map { it.key })
        boosted += "rb#1"
        assertEquals(listOf("rb#1", "ra#1"), source.candidates(from, to).map { it.key })
        assertEquals("rb#1", ready(source.resolve(from, to)).key)
        boosted.clear()
        assertEquals(listOf("ra#1", "rb#1"), source.candidates(from, to).map { it.key })
    }

    @Test
    fun everyPreferenceKeepsWorkingOnOsmData() {
        for (preference in TripPreference.values()) {
            val trips = osmSource.candidates(place(osm, "osm-p1"), place(osm, "osm-p4"), preference)
            assertEquals(preference.name, 1, trips.size)
        }
    }
}
