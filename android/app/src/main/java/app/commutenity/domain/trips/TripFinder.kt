package app.commutenity.domain.trips

enum class Preference { Default, Fastest, Cheapest, FewestTransfers }

data class TripRequest(
    val originStopIds: Set<String>,
    val destinationStopIds: Set<String>,
    val preference: Preference = Preference.Default,
)

data class TripGraph(val edges: List<TripEdge>)

sealed interface TripEdge {
    val id: String
    val fromStopId: String
    val toStopId: String
    val minutes: Int
    val distanceMeters: Int
}

data class RideEdge(
    override val id: String,
    val routeId: String,
    override val fromStopId: String,
    override val toStopId: String,
    override val minutes: Int,
    val farePhp: Int?,
    override val distanceMeters: Int = 0,
    val signboards: List<String> = emptyList(),
) : TripEdge

data class WalkEdge(
    override val id: String,
    override val fromStopId: String,
    override val toStopId: String,
    override val minutes: Int,
    override val distanceMeters: Int = 0,
) : TripEdge

data class TripCandidate(
    val candidateKey: String,
    val legs: List<TripEdge>,
    val totalMinutes: Int,
    val totalFarePhp: Int?,
    val totalDistanceMeters: Int,
    val transfers: Int,
    val walkMinutes: Int,
)

/** Finds and ranks loopless trips without relying on Android or a network connection. */
class TripFinder(private val graph: TripGraph) {
    private val outgoing = graph.edges.groupBy { it.fromStopId }

    fun find(request: TripRequest): List<TripCandidate> {
        val candidates = buildList {
            request.originStopIds.sorted().forEach { origin ->
                visit(
                    stopId = origin,
                    destinations = request.destinationStopIds,
                    path = emptyList(),
                    visitedStops = setOf(origin),
                    candidates = this,
                )
            }
        }
        return candidates.sortedWith(candidateComparator(request.preference)).take(MAX_CANDIDATES)
    }

    private fun visit(
        stopId: String,
        destinations: Set<String>,
        path: List<TripEdge>,
        visitedStops: Set<String>,
        candidates: MutableList<TripCandidate>,
    ) {
        if (stopId in destinations && path.isNotEmpty()) {
            candidates += path.toCandidate()
            return
        }
        outgoing[stopId].orEmpty().forEach { edge ->
            if (edge.toStopId !in visitedStops) {
                visit(
                    stopId = edge.toStopId,
                    destinations = destinations,
                    path = path + edge,
                    visitedStops = visitedStops + edge.toStopId,
                    candidates = candidates,
                )
            }
        }
    }

    private fun List<TripEdge>.toCandidate(): TripCandidate {
        val rides = filterIsInstance<RideEdge>()
        val routeChanges = rides.zipWithNext().count { (first, second) -> first.routeId != second.routeId }
        val hasUnknownFare = rides.any { it.farePhp == null }
        return TripCandidate(
            candidateKey = joinToString("|") { it.id },
            legs = this,
            totalMinutes = sumOf { it.minutes },
            totalFarePhp = if (hasUnknownFare) null else rides.sumOf { it.farePhp!! },
            totalDistanceMeters = sumOf { it.distanceMeters },
            transfers = routeChanges,
            walkMinutes = filterIsInstance<WalkEdge>().sumOf { it.minutes },
        )
    }

    private fun candidateComparator(preference: Preference): Comparator<TripCandidate> {
        fun fare(candidate: TripCandidate) = candidate.totalFarePhp ?: Int.MAX_VALUE
        return when (preference) {
            Preference.Fastest -> compareBy<TripCandidate> { it.totalMinutes }
                .thenBy { it.transfers }
                .thenBy(::fare)
                .thenBy { it.walkMinutes }
                .thenBy { it.candidateKey }
            Preference.Cheapest -> compareBy<TripCandidate>(::fare)
                .thenBy { it.transfers }
                .thenBy { it.totalMinutes }
                .thenBy { it.walkMinutes }
                .thenBy { it.candidateKey }
            Preference.Default, Preference.FewestTransfers -> compareBy<TripCandidate> { it.transfers }
                .thenBy { it.totalMinutes }
                .thenBy(::fare)
                .thenBy { it.walkMinutes }
                .thenBy { it.candidateKey }
        }
    }

    private companion object {
        const val MAX_CANDIDATES = 10
    }
}
