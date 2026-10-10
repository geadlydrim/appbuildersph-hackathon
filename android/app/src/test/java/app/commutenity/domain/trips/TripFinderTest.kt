package app.commutenity.domain.trips

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TripFinderTest {
    private val finderGraph = TripGraph(
        listOf(
            RideEdge("direct", "direct-jeep", "a", "d", minutes = 20, farePhp = 13),
            RideEdge("fast-a", "fast-jeep", "a", "b", minutes = 5, farePhp = 10),
            RideEdge("fast-b", "fast-bus", "b", "d", minutes = 5, farePhp = 10),
            RideEdge("cheap-a", "cheap-jeep", "a", "c", minutes = 12, farePhp = 4),
            RideEdge("cheap-b", "cheap-bus", "c", "d", minutes = 13, farePhp = 4),
            RideEdge("unknown-fare", "unknown-jeep", "a", "e", minutes = 4, farePhp = null),
            RideEdge("unknown-fare-end", "unknown-bus", "e", "d", minutes = 4, farePhp = null),
        ),
    )

    @Test
    fun walkingToAndFromStopsCountsTowardTheBestTrip() {
        // A faster ride from a stop 10 minutes' walk away must lose to a slower ride from a stop next door.
        val graph = TripGraph(
            listOf(
                RideEdge("near-ride", "near-bus", "near", "end", minutes = 20, farePhp = 15),
                RideEdge("far-ride", "far-bus", "far", "end", minutes = 15, farePhp = 15),
            ),
        )
        val request = TripRequest(
            originStopIds = setOf("near", "far"),
            destinationStopIds = setOf("end"),
            originWalkMinutes = mapOf("near" to 1, "far" to 10),
            destinationWalkMinutes = mapOf("end" to 2),
        )

        val found = TripFinder(graph).find(request)

        assertEquals(listOf("near-ride", "far-ride"), found.map { it.candidateKey })
        assertEquals(23, found.first().totalMinutes)
        assertEquals(3, found.first().walkMinutes)
        assertEquals(27, found[1].totalMinutes)
    }

    @Test
    fun theWalkFromTheLastStopDecidesBetweenDestinationStops() {
        val graph = TripGraph(
            listOf(
                RideEdge("to-far-end", "bus-1", "start", "far-end", minutes = 10, farePhp = 15),
                RideEdge("to-near-end", "bus-2", "start", "near-end", minutes = 12, farePhp = 15),
            ),
        )
        val request = TripRequest(
            originStopIds = setOf("start"),
            destinationStopIds = setOf("far-end", "near-end"),
            destinationWalkMinutes = mapOf("far-end" to 9, "near-end" to 1),
        )

        assertEquals("to-near-end", TripFinder(graph).find(request).first().candidateKey)
    }
    private val finder = TripFinder(finderGraph)

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

    @Test
    fun netVotesBreakTiesAfterWalkMinutesAndBeforeCandidateKey() {
        val graph = TripGraph(
            listOf(
                RideEdge("alpha", "route-alpha", "a", "d", minutes = 10, farePhp = 13),
                RideEdge("beta", "route-beta", "a", "d", minutes = 10, farePhp = 13),
            ),
        )
        val request = TripRequest(setOf("a"), setOf("d"))

        assertEquals(listOf("alpha", "beta"), TripFinder(graph).find(request).map { it.candidateKey })
        assertEquals(
            listOf("beta", "alpha"),
            TripFinder(graph, netVotes = { key -> if (key == "beta") 2 else 0 }).find(request).map { it.candidateKey },
        )
    }

    @Test
    fun netVotesAreClampedToPlusMinusThree() {
        val graph = TripGraph(
            listOf(
                RideEdge("alpha", "route-alpha", "a", "d", minutes = 10, farePhp = 13),
                RideEdge("beta", "route-beta", "a", "d", minutes = 10, farePhp = 13),
            ),
        )
        val request = TripRequest(setOf("a"), setOf("d"))

        val highVotes = TripFinder(graph, netVotes = { key -> if (key == "beta") 10 else 3 }).find(request)
        val lowVotes = TripFinder(graph, netVotes = { key -> if (key == "alpha") -10 else -3 }).find(request)

        assertEquals(listOf("alpha", "beta"), highVotes.map { it.candidateKey })
        assertEquals(listOf("alpha", "beta"), lowVotes.map { it.candidateKey })
    }

    @Test
    fun votesNeverOverrideEarlierCriteria() {
        val graph = TripGraph(
            listOf(
                RideEdge("slow", "route-slow", "a", "d", minutes = 20, farePhp = 13),
                RideEdge("quick", "route-quick", "a", "d", minutes = 10, farePhp = 13),
            ),
        )

        val result = TripFinder(graph, netVotes = { key -> if (key == "slow") 3 else -3 })
            .find(TripRequest(setOf("a"), setOf("d")))

        assertEquals(listOf("quick", "slow"), result.map { it.candidateKey })
    }

    @Test
    fun parallelEdgesOnTheSameStopsYieldSeparateCandidates() {
        val graph = TripGraph(
            listOf(
                RideEdge("bus-edge", "bus-1", "a", "b", minutes = 7, farePhp = 15),
                RideEdge("jeep-edge", "jeep-1", "a", "b", minutes = 7, farePhp = 14),
            ),
        )
        val finder = TripFinder(graph)

        listOf(Preference.Default, Preference.Cheapest).forEach { preference ->
            val result = finder.find(TripRequest(setOf("a"), setOf("b"), preference))

            assertEquals(listOf("jeep-edge", "bus-edge"), result.map { it.candidateKey })
            assertEquals(listOf(14, 15), result.map { it.totalFarePhp })
        }
    }

    @Test
    fun denseLayeredGraphIsBoundedAndKeepsTheMinimumCostPathFirst() {
        val layers = 8
        val width = 4
        fun stop(layer: Int, index: Int) = "s$layer-$index"
        fun minutes(layer: Int, from: Int, to: Int) = 1 + (layer * 5 + from * 3 + to * 7) % 9
        val edges = (0 until layers - 1).flatMap { layer ->
            (0 until width).flatMap { from ->
                (0 until width).map { to ->
                    RideEdge(
                        id = "e$layer-$from-$to",
                        routeId = "route-$layer",
                        fromStopId = stop(layer, from),
                        toStopId = stop(layer + 1, to),
                        minutes = minutes(layer, from, to),
                        farePhp = 10,
                    )
                }
            }
        }
        // Every path crosses the same 7 route changes, so the cheapest path is the fewest-minutes one.
        var best = IntArray(width)
        for (layer in 0 until layers - 1) {
            val previous = best
            best = IntArray(width) { to -> (0 until width).minOf { from -> previous[from] + minutes(layer, from, to) } }
        }
        val expectedMinimum = best.minOf { it }

        val result = TripFinder(TripGraph(edges)).find(
            TripRequest(
                originStopIds = (0 until width).map { stop(0, it) }.toSet(),
                destinationStopIds = (0 until width).map { stop(layers - 1, it) }.toSet(),
                preference = Preference.Fastest,
            ),
        )

        assertEquals(10, result.size)
        assertEquals(expectedMinimum, result.first().totalMinutes)
        assertEquals(result.size, result.map { it.candidateKey }.toSet().size)
        assertEquals(true, result.all { candidate -> candidate.legs.size == layers - 1 })
    }

    @Test
    fun transferPenaltyDecidesWhichCandidatesSurviveTheCap() {
        val directs = (20..29).map { minutes ->
            RideEdge("direct-$minutes", "line-$minutes", "a", "z", minutes = minutes, farePhp = 10)
        }
        // 26 raw minutes beats direct-29, but 26 + 5 for the transfer costs more than every direct edge.
        val transferring = listOf(
            RideEdge("hop-1", "line-1", "a", "m", minutes = 13, farePhp = 10),
            RideEdge("hop-2", "line-2", "m", "z", minutes = 13, farePhp = 10),
        )

        val result = TripFinder(TripGraph(directs + transferring)).find(TripRequest(setOf("a"), setOf("z")))

        assertEquals(10, result.size)
        assertEquals(directs.map { it.id }, result.map { it.candidateKey })
    }

    @Test
    fun findEqualsRankOfGenerateForEveryPreference() {
        val request = TripRequest(setOf("a"), setOf("d"))
        val voted = TripFinder(finderGraph, netVotes = { key -> if (key == "unknown-fare|unknown-fare-end") 2 else 0 })
        for (subject in listOf(finder, voted)) {
            val generated = subject.generate(request)
            for (preference in Preference.values()) {
                assertEquals(
                    preference.name,
                    subject.find(request.copy(preference = preference)),
                    subject.rank(generated, preference),
                )
            }
        }
    }

    @Test
    fun estimatedMinutesFlowToTheCandidate() {
        val graph = TripGraph(
            listOf(
                RideEdge("est", "route-est", "a", "b", minutes = 4, farePhp = null, minutesEstimated = true),
                RideEdge("known", "route-known", "a", "b", minutes = 9, farePhp = 12),
            ),
        )

        val byKey = TripFinder(graph).find(TripRequest(setOf("a"), setOf("b"))).associateBy { it.candidateKey }

        assertEquals(true, byKey.getValue("est").minutesEstimated)
        assertEquals(false, byKey.getValue("known").minutesEstimated)
    }

    @Test
    fun aTransferOnlyWalkJoinsTwoRidesButNeverStartsOrEndsATrip() {
        val graph = TripGraph(
            listOf(
                RideEdge("ride-1", "route-1", "a", "b", minutes = 5, farePhp = null),
                WalkEdge("walk-b-c", "b", "c", minutes = 2, distanceMeters = 150, transferOnly = true),
                WalkEdge("walk-c-b", "c", "b", minutes = 2, distanceMeters = 150, transferOnly = true),
                WalkEdge("walk-x-a", "x", "a", minutes = 2, distanceMeters = 150, transferOnly = true),
                RideEdge("ride-2", "route-2", "c", "d", minutes = 5, farePhp = null),
            ),
        )
        val finder = TripFinder(graph)

        assertEquals(listOf("ride-1|walk-b-c|ride-2"), finder.find(TripRequest(setOf("a"), setOf("d"))).map { it.candidateKey })
        assertEquals(emptyList<TripCandidate>(), finder.find(TripRequest(setOf("b"), setOf("c"))))
        assertEquals(emptyList<TripCandidate>(), finder.find(TripRequest(setOf("x"), setOf("b"))))
    }

    private val twoRoutes = TripGraph(
        listOf(
            RideEdge("r1-1", "route-1", "a", "b", minutes = 5, farePhp = 7, distanceMeters = 1000),
            RideEdge("r1-2", "route-1", "b", "c", minutes = 5, farePhp = 7, distanceMeters = 1500),
            RideEdge("r2-1", "route-2", "c", "d", minutes = 5, farePhp = 9, distanceMeters = 2000),
        ),
    )

    @Test
    fun defaultFareSumsEveryEdge() {
        assertEquals(23, TripFinder(twoRoutes).find(TripRequest(setOf("a"), setOf("d"))).single().totalFarePhp)
    }

    @Test
    fun rideFareIsAppliedOncePerRunOfOneRoute() {
        val runs = mutableListOf<Pair<String, List<String>>>()
        val finder = TripFinder(twoRoutes, rideFare = { routeId, rides ->
            runs += routeId to rides.map { it.id }
            if (routeId == "route-1") 18 else rides.sumOf { it.farePhp!! }
        })

        val candidate = finder.find(TripRequest(setOf("a"), setOf("d"))).single()

        assertEquals(27, candidate.totalFarePhp)
        assertEquals(listOf("route-1" to listOf("r1-1", "r1-2"), "route-2" to listOf("r2-1")), runs)
    }

    @Test
    fun aNullRideFareMakesTheTotalUnknown() {
        val finder = TripFinder(twoRoutes, rideFare = { routeId, _ -> if (routeId == "route-1") null else 9 })
        assertNull(finder.find(TripRequest(setOf("a"), setOf("d"))).single().totalFarePhp)
    }

    @Test
    fun aWalkBetweenTwoRidesOnOneRouteEndsTheRun() {
        val graph = TripGraph(
            listOf(
                RideEdge("first", "route-1", "a", "b", minutes = 5, farePhp = 7),
                WalkEdge("walk", "b", "c", minutes = 2, transferOnly = true),
                RideEdge("second", "route-1", "c", "d", minutes = 5, farePhp = 7),
            ),
        )
        var runs = 0
        val finder = TripFinder(graph, rideFare = { _, rides -> runs++; rides.sumOf { it.farePhp!! } })

        assertEquals(14, finder.find(TripRequest(setOf("a"), setOf("d"))).single().totalFarePhp)
        assertEquals(2, runs)
    }
}
