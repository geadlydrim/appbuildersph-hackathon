package app.commutenity.domain

import app.commutenity.data.pack.CommutePack
import app.commutenity.data.pack.PackTripSource
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TripAnswerTest {
    private val pack = CommutePack.parse(File("../../data/pack/hero-trip.source.json").readText())
    private val source = PackTripSource(pack)
    private val rufino = pack.places.first { it.id == "va-rufino" }.toPlace()
    private val delaRosa = pack.places.first { it.id == "dela-rosa-pio-del-pilar" }.toPlace()

    private val trip: Trip = (source.resolve(rufino, delaRosa) as TripResult.Ready).trip
    private val ride = trip.legs.filterIsInstance<Leg.Ride>().single()

    @Test
    fun heroTripFillsTheStructuredRideFields() {
        assertEquals("jeepney", ride.mode)
        assertEquals("Gil Puyat Ave (San Antonio)", ride.board)
        assertEquals("Gil Puyat Ave near Osmeña Hwy / PNR", ride.alight)
        assertEquals("₱12", ride.fare)
        assertEquals("7 min", ride.minutes)
        assertEquals("7 min", (trip.legs.first() as Leg.Walk).minutes)
    }

    @Test
    fun englishAnswerForTheHeroTrip() {
        assertEquals(
            "Walk ~540 m to Gil Puyat Ave (San Antonio). " +
                "Ride the jeep marked \"LRT\". " +
                "Get off at Gil Puyat Ave near Osmeña Hwy / PNR (say \"para\"). " +
                "Walk ~100 m to your stop. " +
                "₱12, about ${trip.minutes} in all.",
            TripAnswer.compose(trip),
        )
    }

    @Test
    fun taglishAnswerForTheHeroTrip() {
        assertEquals(
            "Maglakad ~540 m papunta sa Gil Puyat Ave (San Antonio). " +
                "Sumakay ng jeep na may karatulang \"LRT\". " +
                "Bumaba sa Gil Puyat Ave near Osmeña Hwy / PNR (sabihin \"para\"). " +
                "Maglakad ~100 m. " +
                "₱12, mga ${trip.minutes} lahat.",
            TripAnswer.compose(trip, taglish = true),
        )
    }

    @Test
    fun everyNumberInTheAnswerComesFromTheTrip() {
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

        for (taglish in listOf(false, true)) {
            val answerNumbers = Regex("\\d+").findAll(TripAnswer.compose(trip, taglish)).map { it.value }.toList()
            assertTrue(answerNumbers.isNotEmpty())
            answerNumbers.forEach { assertTrue("$it is not in the trip's fields", it in tripNumbers) }
        }
    }

    @Test
    fun placesAndSignboardComeFromTheTrip() {
        val english = TripAnswer.compose(trip)
        assertTrue(english.contains(ride.board))
        assertTrue(english.contains(ride.alight))
        assertTrue(english.contains("\"${ride.signboards.first()}\""))
    }

    @Test
    fun aBusRideUsesTheBusWord() {
        val bus = trip.copy(legs = trip.legs.map { if (it is Leg.Ride) it.copy(mode = "bus") else it })
        assertTrue(TripAnswer.compose(bus).contains("Ride the bus marked"))
        assertTrue(TripAnswer.compose(bus, taglish = true).contains("Sumakay ng bus na may"))
    }

    @Test
    fun singleSignboardFallsBackWhenTheListIsEmpty() {
        val legs = trip.legs.map { if (it is Leg.Ride) it.copy(signboards = emptyList(), signboard = "Buendia") else it }
        assertTrue(TripAnswer.compose(trip.copy(legs = legs)).contains("marked \"Buendia\"."))
    }

    @Test
    fun noSignboardDropsTheMarkedPart() {
        val legs = trip.legs.map { if (it is Leg.Ride) it.copy(signboards = emptyList(), signboard = "") else it }
        val answer = TripAnswer.compose(trip.copy(legs = legs))
        assertTrue(answer.contains("Ride the jeep. Get off at"))
        assertTrue(!answer.contains("marked"))
    }

    @Test
    fun absentWalksAreLeftOut() {
        val answer = TripAnswer.compose(trip.copy(legs = trip.legs.filterNot { it is Leg.Walk }))
        assertTrue(!answer.contains("Walk"))
        assertTrue(answer.startsWith("Ride the jeep"))
    }

    @Test
    fun severalRidesRepeatTheRideSentence() {
        val second = ride.copy(mode = "bus", board = ride.alight, alight = "Taft Ave")
        val legs = listOf(trip.legs[0], ride, second, trip.legs[2], trip.legs[3])
        val answer = TripAnswer.compose(trip.copy(legs = legs))
        assertTrue(answer.contains("Then ride the bus marked \"LRT\". Get off at Taft Ave (say \"para\")."))
        assertEquals(1, Regex("Walk ~540 m").findAll(answer).count())
    }

    @Test
    fun aSampleTripIsPrefixed() {
        val sample = trip.copy(sample = true)
        assertTrue(TripAnswer.compose(sample).startsWith("Sample: Walk ~540 m"))
        assertTrue(TripAnswer.compose(sample, taglish = true).startsWith("Sample: Maglakad"))
        assertTrue(!TripAnswer.compose(trip).startsWith("Sample"))
    }

    @Test
    fun anUnverifiedTripIsPrefixedWithItsSource() {
        val osm = trip.copy(unverified = true)
        assertTrue(TripAnswer.compose(osm).startsWith("Unverified (OpenStreetMap): Walk ~540 m"))
        assertTrue(TripAnswer.compose(osm, taglish = true).startsWith("Hindi pa beripikado (OpenStreetMap): Maglakad"))
        assertTrue(!TripAnswer.compose(trip).contains("Unverified"))
    }

    @Test
    fun estimatedMinutesKeepTheirTildeAndLabel() {
        val estimated = trip.copy(minutes = "~9 min (est.)", fare = "Fare unknown")
        assertTrue(TripAnswer.compose(estimated).endsWith("Fare unknown, ~9 min (est.) in all."))
        assertTrue(TripAnswer.compose(estimated, taglish = true).endsWith("Fare unknown, ~9 min (est.) lahat."))
    }

    @Test
    fun theWalkOnlyTripSaysNoRideIsNeeded() {
        val start = Place("start", "Start", "", inMakati = true, lat = 14.5613, lng = 121.0149)
        val end = Place("end", "End", "", inMakati = true, lat = 14.5613, lng = 121.0232629)
        val walkOnly = (source.resolve(start, end) as TripResult.Ready).trip

        assertEquals("walk", walkOnly.key)
        assertEquals("Walk ~900 m to your stop, about 12 min. No ride needed.", TripAnswer.compose(walkOnly))
        assertEquals(
            "Maglakad ~900 m papunta sa pupuntahan mo, mga 12 min. Hindi na kailangang sumakay.",
            TripAnswer.compose(walkOnly, taglish = true),
        )
    }

    @Test
    fun aRideLessTripThatIsNotTheWalkOnlyTripStaysEmpty() {
        val walkLegsOnly = trip.copy(legs = listOf(Leg.Walk("~540 m", "7 min"), Leg.Para("Somewhere")))
        assertEquals("", TripAnswer.compose(walkLegsOnly))
    }

    @Test
    fun aTripWithNoRidesGivesNothing() {
        val noRides = trip.copy(legs = trip.legs.filterNot { it is Leg.Ride })
        assertEquals("", TripAnswer.compose(noRides))
        assertEquals("", TripAnswer.compose(noRides, taglish = true))
    }

    @Test
    fun placeholderRideWithoutStopNamesGivesNothing() {
        val placeholder = trip.copy(legs = listOf(Leg.Walk("~XX m"), Leg.Ride("A  →  B", "₱XX  ·  XX min", "TBD"), Leg.Para("TBD")))
        assertEquals("", TripAnswer.compose(placeholder))
    }

    @Test
    fun unknownTimeIsNotPrintedAsATime() {
        val answer = TripAnswer.compose(trip.copy(minutes = "Time unknown"))
        assertTrue(answer.endsWith("₱12."))
    }
}
