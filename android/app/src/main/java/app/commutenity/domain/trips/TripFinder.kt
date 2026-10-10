package app.commutenity.domain.trips

import java.util.PriorityQueue

enum class Preference { Default, Fastest, Cheapest, FewestTransfers }

data class TripRequest(
    val originStopIds: Set<String>,
    val destinationStopIds: Set<String>,
    val preference: Preference = Preference.Default,
    /** Minutes to walk from the rider's start to each origin stop; counted in every trip's total. */
    val originWalkMinutes: Map<String, Int> = emptyMap(),
    /** Minutes to walk from each destination stop to the rider's end; counted in every trip's total. */
    val destinationWalkMinutes: Map<String, Int> = emptyMap(),
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
    /** True when [minutes] is an estimate (no measured time in the data). */
    val minutesEstimated: Boolean = false,
) : TripEdge

data class WalkEdge(
    override val id: String,
    override val fromStopId: String,
    override val toStopId: String,
    override val minutes: Int,
    override val distanceMeters: Int = 0,
    /** A transfer link: only usable between two rides, never as the first or last edge of a trip. */
    val transferOnly: Boolean = false,
) : TripEdge

data class TripCandidate(
    val candidateKey: String,
    val legs: List<TripEdge>,
    val totalMinutes: Int,
    val totalFarePhp: Int?,
    val totalDistanceMeters: Int,
    val transfers: Int,
    val walkMinutes: Int,
    /** True when any ride's minutes are estimates. */
    val minutesEstimated: Boolean = false,
)

/**
 * Finds and ranks loopless trips without relying on Android or a network connection.
 *
 * [rideFare] prices one ride: a run of consecutive ride edges on [routeId]. A null result means the fare is unknown.
 * The default adds up the edges' own fares.
 */
class TripFinder(
    private val graph: TripGraph,
    private val netVotes: (candidateKey: String) -> Int = { 0 },
    private val rideFare: (routeId: String, rides: List<RideEdge>) -> Int? = { _, rides ->
        if (rides.any { it.farePhp == null }) null else rides.sumOf { it.farePhp!! }
    },
) {
    private val edges = graph.edges
    private val outgoing: Map<String, List<Int>> = edges.indices.groupBy { edges[it].fromStopId }

    /** Generates, ranks and caps the candidates for [request]. */
    fun find(request: TripRequest): List<TripCandidate> = rank(generate(request), request.preference)

    /**
     * Every loopless candidate for [request], unranked. Depends only on the graph and the request's stops, so
     * the result can be cached and re-ranked with [rank] as votes change.
     */
    fun generate(request: TripRequest): List<TripCandidate> = kShortestPaths(request).map { it.toCandidate(rideFare) }

    /** Orders [candidates] for [preference] using the current net votes and keeps the best few. */
    fun rank(candidates: List<TripCandidate>, preference: Preference): List<TripCandidate> =
        candidates.sortedWith(candidateComparator(preference)).take(MAX_CANDIDATES)

    /**
     * Yen's k-shortest loopless paths over edge sequences (parallel edges between the same stops stay distinct),
     * costed as minutes + [TRANSFER_PENALTY_MINUTES] per transfer. Multiple origins act as a virtual source.
     */
    private fun kShortestPaths(request: TripRequest): List<RoutedPath> {
        val origins = request.originStopIds.sorted()
        val destinations = request.destinationStopIds
        val access = request.originWalkMinutes
        val egress = request.destinationWalkMinutes
        val first = shortestPath(
            starts = origins.map { SearchState(it, null) },
            destinations = destinations,
            blockedStops = emptySet(),
            blockedEdges = emptySet(),
            startCost = access,
            egress = egress,
        ) ?: return emptyList()

        fun routedPath(origin: String, edgeIndices: List<Int>): RoutedPath {
            val legs = edgeIndices.map { edges[it] }
            val walkIn = access[origin] ?: 0
            val walkOut = egress[legs.last().toStopId] ?: 0
            return RoutedPath(origin, edgeIndices, legs, walkIn, walkOut)
        }

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
                    startCost = access,
                    egress = egress,
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
                    egress = egress,
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
        startCost: Map<String, Int> = emptyMap(),
        egress: Map<String, Int> = emptyMap(),
    ): SpurPath? {
        val queue = PriorityQueue<QueueEntry>(
            compareBy<QueueEntry> { it.cost }.thenBy { it.hops }.thenBy { it.order },
        )
        val labels = HashMap<SearchState, Label>()
        val steps = HashMap<SearchState, Step>()
        // Reaching a destination stop isn't the end: the walk from it to the rider's end still counts. Each
        // arrival is queued as a "finished" entry with that walk added, and the cheapest finished entry wins.
        val finished = HashMap<SearchState, Int>()
        var order = 0L
        starts.forEach { start ->
            val cost = startCost[start.stopId] ?: 0
            labels[start] = Label(cost, 0)
            queue += QueueEntry(start, cost, 0, order++)
        }
        while (queue.isNotEmpty()) {
            val entry = queue.poll()
            if (entry.finish) {
                val edgeIndices = ArrayDeque<Int>()
                var state = entry.state
                while (true) {
                    val step = steps[state] ?: break
                    edgeIndices.addFirst(step.edgeIndex)
                    state = step.previous
                }
                return SpurPath(state.stopId, edgeIndices.toList())
            }
            val label = labels.getValue(entry.state)
            if (entry.cost != label.cost || entry.hops != label.hops) continue
            if (entry.hops > 0 && entry.state.stopId in destinations && entry.state !in finished) {
                val total = entry.cost + (egress[entry.state.stopId] ?: 0)
                finished[entry.state] = total
                queue += QueueEntry(entry.state, total, entry.hops, order++, finish = true)
            }
            for (edgeIndex in outgoing[entry.state.stopId].orEmpty()) {
                if (edgeIndex in blockedEdges) continue
                val edge = edges[edgeIndex]
                if (edge is WalkEdge && edge.transferOnly &&
                    (entry.state.lastRouteId == null || edge.toStopId in destinations)
                ) {
                    continue
                }
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

    private class RoutedPath(
        val origin: String,
        val edgeIndices: List<Int>,
        private val legs: List<TripEdge>,
        private val walkIn: Int,
        private val walkOut: Int,
    ) {
        val cost = walkIn + legs.sumOf { it.minutes } + TRANSFER_PENALTY_MINUTES * legs.transferCount() + walkOut
        private val key = legs.joinToString("|") { it.id }

        fun toCandidate(rideFare: (String, List<RideEdge>) -> Int?): TripCandidate {
            val rides = legs.filterIsInstance<RideEdge>()
            var totalFare: Int? = 0
            for (run in legs.rideRuns()) {
                val fare = rideFare(run.first().routeId, run)
                totalFare = if (fare == null || totalFare == null) null else totalFare + fare
            }
            return TripCandidate(
                candidateKey = key,
                legs = legs,
                totalMinutes = walkIn + legs.sumOf { it.minutes } + walkOut,
                totalFarePhp = totalFare,
                totalDistanceMeters = legs.sumOf { it.distanceMeters },
                transfers = legs.transferCount(),
                walkMinutes = walkIn + legs.filterIsInstance<WalkEdge>().sumOf { it.minutes } + walkOut,
                minutesEstimated = rides.any { it.minutesEstimated },
            )
        }
    }

    private class SpurPath(val startStopId: String, val edgeIndices: List<Int>)

    private data class SearchState(val stopId: String, val lastRouteId: String?)

    private class Label(val cost: Int, val hops: Int)

    private class Step(val previous: SearchState, val edgeIndex: Int)

    /** [finish] marks an arrival at a destination with the final walk added; popping one ends the search. */
    private class QueueEntry(
        val state: SearchState,
        val cost: Int,
        val hops: Int,
        val order: Long,
        val finish: Boolean = false,
    )

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

        /** Consecutive ride edges on the same route, grouped: each run is one ride. A walk or a route change ends a run. */
        fun List<TripEdge>.rideRuns(): List<List<RideEdge>> {
            val runs = mutableListOf<MutableList<RideEdge>>()
            var previous: TripEdge? = null
            for (edge in this) {
                if (edge is RideEdge) {
                    val last = previous as? RideEdge
                    if (last != null && last.routeId == edge.routeId) runs.last().add(edge) else runs += mutableListOf(edge)
                }
                previous = edge
            }
            return runs
        }
    }
}
