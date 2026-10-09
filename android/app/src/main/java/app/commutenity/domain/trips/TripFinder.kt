package app.commutenity.domain.trips

import java.util.PriorityQueue

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
class TripFinder(
    private val graph: TripGraph,
    private val netVotes: (candidateKey: String) -> Int = { 0 },
) {
    private val edges = graph.edges
    private val outgoing: Map<String, List<Int>> = edges.indices.groupBy { edges[it].fromStopId }

    fun find(request: TripRequest): List<TripCandidate> =
        kShortestPaths(request)
            .map { it.toCandidate() }
            .sortedWith(candidateComparator(request.preference))
            .take(MAX_CANDIDATES)

    /**
     * Yen's k-shortest loopless paths over edge sequences (parallel edges between the same stops stay distinct),
     * costed as minutes + [TRANSFER_PENALTY_MINUTES] per transfer. Multiple origins act as a virtual source.
     */
    private fun kShortestPaths(request: TripRequest): List<RoutedPath> {
        val origins = request.originStopIds.sorted()
        val destinations = request.destinationStopIds
        val first = shortestPath(
            starts = origins.map { SearchState(it, null) },
            destinations = destinations,
            blockedStops = emptySet(),
            blockedEdges = emptySet(),
        ) ?: return emptyList()

        val accepted = mutableListOf(routedPath(first.startStopId, first.edgeIndices))
        val seen = hashSetOf(first.edgeIndices)
        val pending = mutableListOf<RoutedPath>()

        fun offer(path: RoutedPath) {
            if (seen.add(path.edgeIndices)) pending += path
        }

        while (accepted.size < MAX_CANDIDATES) {
            val previous = accepted.last()

            // Deviate at the virtual source: start from an origin no accepted path has used yet.
            val usedOrigins = accepted.mapTo(hashSetOf()) { it.origin }
            val freshOrigins = origins.filter { it !in usedOrigins }
            if (freshOrigins.isNotEmpty()) {
                shortestPath(
                    starts = freshOrigins.map { SearchState(it, null) },
                    destinations = destinations,
                    blockedStops = emptySet(),
                    blockedEdges = emptySet(),
                )?.let { offer(routedPath(it.startStopId, it.edgeIndices)) }
            }

            // Deviate at each stop of the previous path, keeping its root fixed.
            for (spurIndex in previous.edgeIndices.indices) {
                val root = previous.edgeIndices.subList(0, spurIndex)
                val rootStops = buildList {
                    add(previous.origin)
                    root.forEach { add(edges[it].toStopId) }
                }
                val blockedEdges = accepted
                    .filter { it.origin == previous.origin && it.edgeIndices.size > spurIndex && it.edgeIndices.subList(0, spurIndex) == root }
                    .mapTo(hashSetOf()) { it.edgeIndices[spurIndex] }
                val lastRouteId = root.asReversed().firstNotNullOfOrNull { (edges[it] as? RideEdge)?.routeId }
                val spur = shortestPath(
                    starts = listOf(SearchState(rootStops.last(), lastRouteId)),
                    destinations = destinations,
                    blockedStops = rootStops.toHashSet(),
                    blockedEdges = blockedEdges,
                ) ?: continue
                offer(routedPath(previous.origin, root + spur.edgeIndices))
            }

            val next = pending.minWithOrNull(PATH_ORDER) ?: break
            pending.remove(next)
            accepted += next
        }
        return accepted
    }

    /**
     * Dijkstra over (stop, last ride route) states so transfer penalties stay additive. Ties on cost prefer fewer
     * hops, which keeps zero-cost detours (loops) out of results. Stops at the first destination reached after at
     * least one edge.
     */
    private fun shortestPath(
        starts: List<SearchState>,
        destinations: Set<String>,
        blockedStops: Set<String>,
        blockedEdges: Set<Int>,
    ): SpurPath? {
        val queue = PriorityQueue<QueueEntry>(
            compareBy<QueueEntry> { it.cost }.thenBy { it.hops }.thenBy { it.order },
        )
        val labels = HashMap<SearchState, Label>()
        val steps = HashMap<SearchState, Step>()
        var order = 0L
        starts.forEach { start ->
            labels[start] = Label(0, 0)
            queue += QueueEntry(start, 0, 0, order++)
        }
        while (queue.isNotEmpty()) {
            val entry = queue.poll()
            val label = labels.getValue(entry.state)
            if (entry.cost != label.cost || entry.hops != label.hops) continue
            if (entry.hops > 0 && entry.state.stopId in destinations) {
                val edgeIndices = ArrayDeque<Int>()
                var state = entry.state
                while (true) {
                    val step = steps[state] ?: break
                    edgeIndices.addFirst(step.edgeIndex)
                    state = step.previous
                }
                return SpurPath(state.stopId, edgeIndices.toList())
            }
            for (edgeIndex in outgoing[entry.state.stopId].orEmpty()) {
                if (edgeIndex in blockedEdges) continue
                val edge = edges[edgeIndex]
                if (edge.toStopId in blockedStops) continue
                val ride = edge as? RideEdge
                val penalty = if (ride != null && entry.state.lastRouteId != null && entry.state.lastRouteId != ride.routeId) {
                    TRANSFER_PENALTY_MINUTES
                } else {
                    0
                }
                val nextState = SearchState(edge.toStopId, ride?.routeId ?: entry.state.lastRouteId)
                val cost = entry.cost + edge.minutes + penalty
                val hops = entry.hops + 1
                val known = labels[nextState]
                if (known == null || cost < known.cost || (cost == known.cost && hops < known.hops)) {
                    labels[nextState] = Label(cost, hops)
                    steps[nextState] = Step(entry.state, edgeIndex)
                    queue += QueueEntry(nextState, cost, hops, order++)
                }
            }
        }
        return null
    }

    private fun routedPath(origin: String, edgeIndices: List<Int>) =
        RoutedPath(origin, edgeIndices, edgeIndices.map { edges[it] })

    private class RoutedPath(val origin: String, val edgeIndices: List<Int>, private val legs: List<TripEdge>) {
        val cost = legs.sumOf { it.minutes } + TRANSFER_PENALTY_MINUTES * legs.transferCount()
        private val key = legs.joinToString("|") { it.id }

        fun toCandidate(): TripCandidate {
            val rides = legs.filterIsInstance<RideEdge>()
            val hasUnknownFare = rides.any { it.farePhp == null }
            return TripCandidate(
                candidateKey = key,
                legs = legs,
                totalMinutes = legs.sumOf { it.minutes },
                totalFarePhp = if (hasUnknownFare) null else rides.sumOf { it.farePhp!! },
                totalDistanceMeters = legs.sumOf { it.distanceMeters },
                transfers = legs.transferCount(),
                walkMinutes = legs.filterIsInstance<WalkEdge>().sumOf { it.minutes },
            )
        }
    }

    private class SpurPath(val startStopId: String, val edgeIndices: List<Int>)

    private data class SearchState(val stopId: String, val lastRouteId: String?)

    private class Label(val cost: Int, val hops: Int)

    private class Step(val previous: SearchState, val edgeIndex: Int)

    private class QueueEntry(val state: SearchState, val cost: Int, val hops: Int, val order: Long)

    private fun candidateComparator(preference: Preference): Comparator<TripCandidate> {
        fun fare(candidate: TripCandidate) = candidate.totalFarePhp ?: Int.MAX_VALUE
        fun votes(candidate: TripCandidate) = netVotes(candidate.candidateKey).coerceIn(-MAX_NET_VOTES, MAX_NET_VOTES)
        return when (preference) {
            Preference.Fastest -> compareBy<TripCandidate> { it.totalMinutes }
                .thenBy { it.transfers }
                .thenBy(::fare)
                .thenBy { it.walkMinutes }
                .thenByDescending(::votes)
                .thenBy { it.candidateKey }
            Preference.Cheapest -> compareBy<TripCandidate>(::fare)
                .thenBy { it.transfers }
                .thenBy { it.totalMinutes }
                .thenBy { it.walkMinutes }
                .thenByDescending(::votes)
                .thenBy { it.candidateKey }
            Preference.Default, Preference.FewestTransfers -> compareBy<TripCandidate> { it.transfers }
                .thenBy { it.totalMinutes }
                .thenBy(::fare)
                .thenBy { it.walkMinutes }
                .thenByDescending(::votes)
                .thenBy { it.candidateKey }
        }
    }

    private companion object {
        const val MAX_CANDIDATES = 10
        const val MAX_NET_VOTES = 3

        /** Cost of each route change when ranking candidate paths, in minutes (D16). */
        const val TRANSFER_PENALTY_MINUTES = 5

        val PATH_ORDER: Comparator<RoutedPath> = compareBy<RoutedPath> { it.cost }
            .thenBy { it.edgeIndices.size }
            .thenBy { it.edgeIndices.joinToString(",") }

        fun List<TripEdge>.transferCount(): Int =
            filterIsInstance<RideEdge>().zipWithNext().count { (first, second) -> first.routeId != second.routeId }
    }
}
