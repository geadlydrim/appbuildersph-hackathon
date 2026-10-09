package app.commutenity.domain.trips

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TripFinderTest {
    private val finder = TripFinder(
        TripGraph(
            listOf(
                RideEdge("direct", "direct-jeep", "a", "d", minutes = 20, farePhp = 13),
                RideEdge("fast-a", "fast-jeep", "a", "b", minutes = 5, farePhp = 10),
                RideEdge("fast-b", "fast-bus", "b", "d", minutes = 5, farePhp = 10),
                RideEdge("cheap-a", "cheap-jeep", "a", "c", minutes = 12, farePhp = 4),
                RideEdge("cheap-b", "cheap-bus", "c", "d", minutes = 13, farePhp = 4),
                RideEdge("unknown-fare", "unknown-jeep", "a", "e", minutes = 4, farePhp = null),
                RideEdge("unknown-fare-end", "unknown-bus", "e", "d", minutes = 4, farePhp = null),
            ),
        ),
    )

    @Test
    fun defaultPrefersFewestTransfersBeforeMinutesAndFare() {
        val result = finder.find(TripRequest(setOf("a"), setOf("d")))

        assertEquals("direct", result.first().candidateKey)
        assertEquals(0, result.first().transfers)
    }

    @Test
    fun fastestPromotesTotalMinutes() {
        val result = finder.find(TripRequest(setOf("a"), setOf("d"), Preference.Fastest))

        assertEquals("unknown-fare|unknown-fare-end", result.first().candidateKey)
        assertEquals(8, result.first().totalMinutes)
    }

    @Test
    fun cheapestPromotesKnownTotalFareAndLeavesUnknownFareUnknown() {
        val result = finder.find(TripRequest(setOf("a"), setOf("d"), Preference.Cheapest))

        assertEquals("cheap-a|cheap-b", result.first().candidateKey)
        assertEquals(8, result.first().totalFarePhp)
        assertNull(result.first { it.candidateKey == "unknown-fare|unknown-fare-end" }.totalFarePhp)
    }

    @Test
    fun preservesRideDetailsAndWalkDistanceForTripDisplay() {
        val finder = TripFinder(
            TripGraph(
                listOf(
                    WalkEdge("first-mile", "a", "board", minutes = 3, distanceMeters = 210),
                    RideEdge(
                        id = "ride",
                        routeId = "jeep-1",
                        fromStopId = "board",
                        toStopId = "alight",
                        minutes = 12,
                        farePhp = 13,
                        distanceMeters = 1_800,
                        signboards = listOf("WASHINGTON AYALA"),
                    ),
                ),
            ),
        )

        val candidate = finder.find(TripRequest(setOf("a"), setOf("alight"))).single()

        assertEquals(15, candidate.totalMinutes)
        assertEquals(3, candidate.walkMinutes)
        assertEquals(2_010, candidate.totalDistanceMeters)
        assertEquals("WASHINGTON AYALA", (candidate.legs[1] as RideEdge).signboards.single())
    }

    @Test
    fun returnsAtMostTenRankedCandidates() {
        val finder = TripFinder(
            TripGraph(
                (1..11).map { number ->
                    RideEdge("route-$number", "route-$number", "a", "d", minutes = number, farePhp = number)
                },
            ),
        )

        val result = finder.find(TripRequest(setOf("a"), setOf("d")))

        assertEquals(10, result.size)
        assertEquals("route-1", result.first().candidateKey)
        assertEquals("route-10", result.last().candidateKey)
    }

    @Test
    fun returnsNoCandidatesWhenStopsAreDisconnected() {
        val result = finder.find(TripRequest(setOf("d"), setOf("a")))

        assertEquals(emptyList<TripCandidate>(), result)
    }
}
