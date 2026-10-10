package app.commutenity.data.pack

import android.content.Context
import android.util.Log
import app.commutenity.domain.Field
import app.commutenity.domain.GeoPoint
import app.commutenity.domain.Leg
import app.commutenity.domain.Place
import app.commutenity.domain.SearchRow
import app.commutenity.domain.Trip
import app.commutenity.domain.TripPath
import app.commutenity.domain.TripPreference
import app.commutenity.domain.TripResult
import app.commutenity.domain.TripSource
import app.commutenity.domain.WALK_ONLY_TRIP_KEY
import app.commutenity.domain.trips.Preference
import app.commutenity.domain.trips.RideEdge
import app.commutenity.domain.trips.TripEdge
import app.commutenity.domain.trips.TripCandidate
import app.commutenity.domain.trips.TripFinder
import app.commutenity.domain.trips.TripGraph
import app.commutenity.domain.trips.TripRequest
import app.commutenity.domain.trips.WalkEdge
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Trips and places straight from the commute pack. Every fare, minute, stop, signboard and shape shown
 * comes from [pack]; nothing is invented here. Minutes of OpenStreetMap rides are estimates and say so.
 */
class PackTripSource(
    private val pack: CommutePack,
    netVotes: (candidateKey: String) -> Int = { 0 },
) : TripSource {
    private val placeIndex = pack.placeIndex()
    private val stopsById = pack.stops.associateBy { it.id }
    private val packPlaceStops = pack.places.associate { it.id to it.stopIds }
    private val segmentsByEdgeId = pack.segments.associateBy { edgeId(it) }
    private val signboardsByRoute = pack.routes.associate { it.id to it.signboards }
    private val modeByRoute = pack.routes.associate { it.id to it.mode }
    private val fareRuleByRoute = pack.routes.mapNotNull { route -> route.fareRule?.let { route.id to it } }.toMap()
    private val graphSegments = pack.segments.filter { it.fromStopId in stopsById && it.toStopId in stopsById }

    /** Everything: every ride plus walking links between nearby stops, so riders can transfer on foot. */
    private val finder: TripFinder

    /** The hero pair only ever sees the team's own (known or collected) rides and no generated walking links. */
    private val heroFinder: TripFinder

    init {
        val allRides = graphSegments.map { rideEdge(it) }
        val heroRides = graphSegments.filter { it.sourceClass == CommutePack.SOURCE_KNOWN || it.sourceClass == CommutePack.SOURCE_COLLECTED }
            .map { rideEdge(it) }
        val linkedStopIds = graphSegments.flatMapTo(hashSetOf()) { listOf(it.fromStopId, it.toStopId) }
        finder = TripFinder(TripGraph(allRides + walkLinks(linkedStopIds)), netVotes, ::fareOfRide)
        heroFinder = TripFinder(TripGraph(heroRides), netVotes, ::fareOfRide)
    }

    /**
     * Fare of one ride (consecutive edges on [routeId]). A route with a `fare_rule` is priced from the ride's total
     * distance; any other route adds up its segments' own fares, unknown when one is missing.
     */
    private fun fareOfRide(routeId: String, ride: List<RideEdge>): Int? {
        val rule = fareRuleByRoute[routeId]
        if (rule != null) return FareRules.fare(rule, ride.sumOf { it.distanceMeters })
        if (ride.any { it.farePhp == null }) return null
        return ride.sumOf { it.farePhp!! }
    }

    /** Unranked finder output per stop pair; ranking (which reads the current votes) runs on every call. */
    private val cache = object : LinkedHashMap<CacheKey, List<TripCandidate>>(CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<CacheKey, List<TripCandidate>>): Boolean =
            size > CACHE_SIZE
    }

    /** Walks depend on where the rider stands, not just which stops are near, so the places are part of the key. */
    private data class CacheKey(
        val origins: Set<String>,
        val destinations: Set<String>,
        val from: GeoPoint?,
        val to: GeoPoint?,
        val hero: Boolean,
    )

    private fun rideEdge(segment: PackSegment) = RideEdge(
        id = edgeId(segment),
        routeId = segment.routeId,
        fromStopId = segment.fromStopId,
        toStopId = segment.toStopId,
        minutes = segment.minutes ?: segment.minutesEst ?: 0,
        farePhp = segment.farePhp,
        distanceMeters = segment.distanceM ?: 0,
        signboards = signboardsByRoute[segment.routeId].orEmpty(),
        minutesEstimated = segment.minutes == null && segment.minutesEst != null,
    )

    /**
     * Walking links, both directions, between distinct stops within [TRANSFER_WALK_M] in a straight line. Each
     * stop links to its [MAX_TRANSFER_LINKS] nearest; a link chosen by either end is added both ways. A lat/lng
     * grid with cells of that size keeps the search local instead of comparing every pair.
     */
    private fun walkLinks(stopIds: Set<String>): List<WalkEdge> {
        val stops = stopIds.sorted().mapNotNull { stopsById[it] }
        if (stops.isEmpty()) return emptyList()
        val cellLat = TRANSFER_WALK_M / METERS_PER_DEGREE
        val cellLng = cellLat / cos(Math.toRadians(stops.map { it.lat }.average()))
        fun row(stop: PackStop) = floor(stop.lat / cellLat).toInt()
        fun col(stop: PackStop) = floor(stop.lng / cellLng).toInt()
        val grid = stops.groupBy { GridCell(row(it), col(it)) }

        val metres = LinkedHashMap<Pair<String, String>, Int>()
        for (stop in stops) {
            val nearest = buildList {
                for (dRow in -1..1) for (dCol in -1..1) {
                    grid[GridCell(row(stop) + dRow, col(stop) + dCol)].orEmpty().forEach { other ->
                        if (other.id == stop.id) return@forEach
                        val distance = haversineMeters(stop.lat, stop.lng, other.lat, other.lng)
                        if (distance <= TRANSFER_WALK_M) add(other to distance)
                    }
                }
            }.sortedWith(compareBy<Pair<PackStop, Double>>({ it.second }, { it.first.id })).take(MAX_TRANSFER_LINKS)
            for ((other, distance) in nearest) {
                val rounded = distance.roundToInt()
                metres.putIfAbsent(stop.id to other.id, rounded)
                metres.putIfAbsent(other.id to stop.id, rounded)
            }
        }
        return metres.map { (pair, distance) ->
            WalkEdge(
                id = "walk:${pair.first}>${pair.second}",
                fromStopId = pair.first,
                toStopId = pair.second,
                minutes = walkMinutes(distance),
                distanceMeters = distance,
                transferOnly = true,
            )
        }
    }

    private data class GridCell(val row: Int, val col: Int)

    /** No GPS yet: the first pack place stands in, and its name says so. */
    override val myLocation: Place = pack.places.first().toPlace().let {
        it.copy(name = "My location (demo: ${it.name})")
    }

    override fun search(field: Field, query: String): List<SearchRow> = buildList<SearchRow> {
        if (field == Field.A) add(SearchRow.UseMyLocation)
        placeIndex.search(query).forEach { add(SearchRow.PlaceRow(it)) }
    }

    override fun resolve(origin: Place, destination: Place, preference: TripPreference): TripResult =
        candidates(origin, destination, preference).firstOrNull()?.let { TripResult.Ready(it) } ?: TripResult.NotInData

    override fun candidates(origin: Place, destination: Place, preference: TripPreference): List<Trip> {
        if (!origin.inMakati || !destination.inMakati) return emptyList()
        val hero = origin.id == HERO_ORIGIN_ID && destination.id == HERO_DESTINATION_ID
        val rideTrips = rideTrips(origin, destination, preference, hero)
        val walkOnly = (if (hero) null else walkOnlyTrip(origin, destination)) ?: return rideTrips.map { it.trip }

        // Walking goes first when no ride exists, when the rider wants the cheapest, or when it is no slower than
        // the best ride; otherwise it is offered right after that ride.
        val bestRide = rideTrips.firstOrNull()
        val walkFirst = bestRide == null || preference == TripPreference.Cheapest ||
            (bestRide.totalMinutes != null && walkOnly.minutes <= bestRide.totalMinutes)
        val trips = rideTrips.map { it.trip }
        return if (walkFirst) listOf(walkOnly.trip) + trips else listOf(trips.first(), walkOnly.trip) + trips.drop(1)
    }

    private fun rideTrips(origin: Place, destination: Place, preference: TripPreference, hero: Boolean): List<BuiltTrip> {
        val originStops = stopsFor(origin)
        val destinationStops = stopsFor(destination)
        if (originStops.isEmpty() || destinationStops.isEmpty()) return emptyList()

        // Count the walk to the first stop and from the last stop while searching, so a ride from a stop 700 m
        // away can't beat one from next door just because its riding part is shorter.
        fun walkTo(place: Place, stopId: String) =
            stopsById[stopId]?.let { walkMinutes(walkMeters(place, it.lat, it.lng)) } ?: 0
        val request = TripRequest(
            originStopIds = originStops,
            destinationStopIds = destinationStops,
            originWalkMinutes = originStops.associateWith { walkTo(origin, it) },
            destinationWalkMinutes = destinationStops.associateWith { walkTo(destination, it) },
        )
        val active = if (hero) heroFinder else finder
        val generated = synchronized(cache) {
            cache.getOrPut(CacheKey(originStops, destinationStops, origin.point(), destination.point(), hero)) {
                active.generate(request)
            }
        }
        val found = active.rank(generated, preference.toFinder())
        return found.mapIndexedNotNull { index, candidate ->
            buildTrip(candidate, reasonFor(candidate, found.getOrNull(index + 1), preference), origin, destination)
        }
    }

    private class BuiltTrip(val trip: Trip, val totalMinutes: Int?)

    private class WalkOnly(val trip: Trip, val minutes: Int)

    /** Walking the whole way, offered when both places have coordinates and are within [WALK_ONLY_MAX_M] in a straight line. */
    private fun walkOnlyTrip(origin: Place, destination: Place): WalkOnly? {
        val from = origin.point() ?: return null
        val to = destination.point() ?: return null
        if (haversineMeters(from.lat, from.lng, to.lat, to.lng) > WALK_ONLY_MAX_M) return null
        val meters = walkMeters(origin, to.lat, to.lng)
        if (meters < MIN_WALK_M) return null
        val minutes = walkMinutes(meters)
        return WalkOnly(
            Trip(
                key = WALK_KEY,
                fare = "₱0",
                minutes = "$minutes min",
                transfers = "0",
                distanceLine = String.format(Locale.US, "%.1f km   ·   walk %d min", meters / 1000.0, minutes),
                reason = "Walk: no ride needed",
                sample = false,
                legs = listOf(Leg.Walk("~$meters m", "$minutes min")),
                path = TripPath(rides = emptyList(), walks = listOf(listOf(from, to)), boardStops = emptyList(), para = null),
                unverified = false,
            ),
            minutes,
        )
    }

    /** A pack place uses its declared stops; any other place with coordinates uses every stop within walking range. */
    private fun stopsFor(place: Place): Set<String> {
        packPlaceStops[place.id]?.filter { it in stopsById }?.takeIf { it.isNotEmpty() }?.let { return it.toSet() }
        val lat = place.lat ?: return emptySet()
        val lng = place.lng ?: return emptySet()
        return pack.stops.filter { haversineMeters(lat, lng, it.lat, it.lng) <= MAX_WALK_M }.mapTo(hashSetOf()) { it.id }
    }

    /** One step of the trip as the rider sees it: a whole ride on one route, or a walk between rides. */
    private fun groupLegs(legs: List<TripEdge>): List<List<TripEdge>> {
        val groups = mutableListOf<MutableList<TripEdge>>()
        for (edge in legs) {
            val previous = groups.lastOrNull()?.last()
            val joins = when {
                previous is RideEdge && edge is RideEdge -> previous.routeId == edge.routeId
                previous is WalkEdge && edge is WalkEdge -> true
                else -> false
            }
            if (joins) groups.last().add(edge) else groups.add(mutableListOf(edge))
        }
        return groups
    }

    private fun minutesText(minutes: Int, estimated: Boolean): String =
        if (estimated) "~$minutes min (est.)" else "$minutes min"

    private fun buildTrip(candidate: TripCandidate, reason: String, origin: Place, destination: Place): BuiltTrip? {
        // Consecutive edges on one route become one ride; walks between rides stay as transfer walks.
        val groups = groupLegs(candidate.legs)
        val firstRideGroup = groups.indexOfFirst { it.first() is RideEdge }
        if (firstRideGroup < 0) return null
        val steps = groups.subList(firstRideGroup, groups.indexOfLast { it.first() is RideEdge } + 1)
        val rides = steps.filter { it.first() is RideEdge }.map { group -> group.map { it as RideEdge } }
        val ridesSegments = rides.map { ride -> ride.map { segmentsByEdgeId[it.id] ?: return null } }

        val board = stopsById[rides.first().first().fromStopId] ?: return null
        val alight = stopsById[rides.last().last().toStopId] ?: return null
        val segments = ridesSegments.flatten()

        val firstWalkM = walkMeters(origin, board.lat, board.lng)
        val lastWalkM = walkMeters(destination, alight.lat, alight.lng)
        val transferWalks = steps.filter { it.first() is WalkEdge }.map { group -> group.map { it as WalkEdge } }
        val transferWalkMinutes = transferWalks.sumOf { walk -> walk.sumOf { it.minutes } }
        val walkMinutes = walkMinutes(firstWalkM) + walkMinutes(lastWalkM) + transferWalkMinutes

        val segmentMinutes = segments.map { it.minutes ?: it.minutesEst }
        val estimated = segments.any { it.minutes == null && it.minutesEst != null }
        val totalMinutes = if (segmentMinutes.any { it == null }) null else walkMinutes + segmentMinutes.sumOf { it!! }
        val minutes = if (totalMinutes == null) "Time unknown" else minutesText(totalMinutes, estimated)
        val fare = candidate.totalFarePhp?.let { "₱$it" } ?: "Fare unknown"
        val kilometres = (
            segments.sumOf { it.distanceM ?: 0 } + transferWalks.sumOf { walk -> walk.sumOf { it.distanceMeters } } +
                firstWalkM + lastWalkM
            ) / 1000.0

        val rideLegs = steps.map { group ->
            val first = group.first()
            if (first is RideEdge) {
                val ride = group.map { it as RideEdge }
                val rideSegments = ride.map { segmentsByEdgeId.getValue(it.id) }
                val from = stopsById.getValue(first.fromStopId)
                val to = stopsById.getValue(ride.last().toStopId)
                val fareText = fareOfRide(first.routeId, ride)?.let { "₱$it" } ?: "Fare unknown"
                val times = rideSegments.map { it.minutes ?: it.minutesEst }
                val rideEstimated = rideSegments.any { it.minutes == null && it.minutesEst != null }
                val minutesLeg = if (times.any { it == null }) "" else minutesText(times.sumOf { it!! }, rideEstimated)
                Leg.Ride(
                    stops = "${from.name}  →  ${to.name}",
                    fareAndMinutes = "$fareText  ·  ${minutesLeg.ifEmpty { "time unknown" }}",
                    signboard = first.signboards.joinToString(" / "),
                    signboards = first.signboards,
                    mode = modeByRoute[first.routeId].orEmpty(),
                    board = from.name,
                    alight = to.name,
                    fare = fareText,
                    minutes = minutesLeg,
                )
            } else {
                val meters = (group.sumOf { it.distanceMeters } / 10.0).roundToInt() * 10
                Leg.Walk("~$meters m", "${group.sumOf { it.minutes }} min")
            }
        }

        val legs = buildList<Leg> {
            if (firstWalkM >= MIN_WALK_M) add(Leg.Walk("~$firstWalkM m", "${walkMinutes(firstWalkM)} min"))
            addAll(rideLegs)
            if (lastWalkM >= MIN_WALK_M) add(Leg.Walk("~$lastWalkM m", "${walkMinutes(lastWalkM)} min"))
            add(Leg.Para(alight.name))
        }

        fun point(stopId: String) = stopsById.getValue(stopId).let { GeoPoint(it.lat, it.lng) }
        val boardPoint = GeoPoint(board.lat, board.lng)
        val alightPoint = GeoPoint(alight.lat, alight.lng)
        val path = TripPath(
            rides = rides.mapIndexed { rideIndex, ride ->
                buildList<GeoPoint> {
                    ride.forEachIndexed { edgeIndex, edge ->
                        val decoded = ridesSegments[rideIndex][edgeIndex].polyline
                            ?.let { Polyline.decode(it, pack.shapePrecision) }.orEmpty()
                        val points = decoded.takeIf { it.size >= 2 } ?: listOf(point(edge.fromStopId), point(edge.toStopId))
                        addAll(if (isNotEmpty() && points.first() == last()) points.drop(1) else points)
                    }
                }
            },
            walks = buildList<List<GeoPoint>> {
                val from = origin.point()
                if (from != null && firstWalkM >= MIN_WALK_M) add(listOf(from, boardPoint))
                transferWalks.forEach { walk -> add(listOf(point(walk.first().fromStopId), point(walk.last().toStopId))) }
                val to = destination.point()
                if (to != null && lastWalkM >= MIN_WALK_M) add(listOf(alightPoint, to))
            },
            boardStops = rides.map { ride -> point(ride.first().fromStopId) },
            para = alightPoint,
        )

        val trip = Trip(
            key = candidate.candidateKey,
            fare = fare,
            minutes = minutes,
            transfers = candidate.transfers.toString(),
            distanceLine = String.format(Locale.US, "%.1f km   ·   walk %d min", kilometres, walkMinutes),
            reason = reason,
            sample = false,
            legs = legs,
            path = path,
            unverified = segments.any { it.sourceClass == CommutePack.SOURCE_OSM },
        )
        return BuiltTrip(trip, totalMinutes)
    }

    /** Straight-line walk to a stop, rounded to 10 m; 0 when the place has no coordinates. */
    private fun walkMeters(place: Place, stopLat: Double, stopLng: Double): Int {
        val lat = place.lat ?: return 0
        val lng = place.lng ?: return 0
        val metres = haversineMeters(lat, lng, stopLat, stopLng)
        return (metres / 10.0).roundToInt() * 10
    }

    private fun walkMinutes(meters: Int): Int =
        if (meters < MIN_WALK_M) 0 else ceil(meters / WALK_M_PER_MIN).toInt()

    private fun Place.point(): GeoPoint? = if (lat != null && lng != null) GeoPoint(lat, lng) else null

    /** Names the first criterion, in the D16 order for [preference], where [best] beats [runnerUp]. */
    private fun reasonFor(best: TripCandidate, runnerUp: TripCandidate?, preference: TripPreference): String {
        if (runnerUp == null) return "Only trip in my data"
        val transfers = "Fewest transfers" to { c: TripCandidate -> c.transfers }
        val minutes = "Fastest" to { c: TripCandidate -> c.totalMinutes }
        val cheapest = "Cheapest" to { c: TripCandidate -> c.totalFarePhp ?: Int.MAX_VALUE }
        val walking = "Least walking" to { c: TripCandidate -> c.walkMinutes }
        val order = when (preference) {
            TripPreference.Fastest -> listOf(minutes, transfers, cheapest, walking)
            TripPreference.Cheapest -> listOf(cheapest, transfers, minutes, walking)
            TripPreference.Default, TripPreference.FewestTransfers -> listOf(transfers, minutes, cheapest, walking)
        }
        return order.firstOrNull { (_, measure) -> measure(best) < measure(runnerUp) }?.first
            ?: "Tied with another trip"
    }

    private fun TripPreference.toFinder(): Preference = when (this) {
        TripPreference.Default -> Preference.Default
        TripPreference.Fastest -> Preference.Fastest
        TripPreference.Cheapest -> Preference.Cheapest
        TripPreference.FewestTransfers -> Preference.FewestTransfers
    }

    companion object {
        const val MAX_WALK_M = 800.0
        const val WALK_M_PER_MIN = 80.0
        private const val MIN_WALK_M = 10
        private const val WALK_ONLY_MAX_M = 1500.0
        private const val WALK_KEY = WALK_ONLY_TRIP_KEY
        private const val HERO_ASSET_PATH = "pack/hero-trip.json"
        private const val OSM_ASSET_PATH = "pack/osm-makati.json"
        private const val CAROUSEL_ASSET_PATH = "pack/carousel-makati.json"
        private const val EARTH_RADIUS_M = 6_371_000.0
        private const val METERS_PER_DEGREE = 111_195.0
        private const val TRANSFER_WALK_M = 200.0
        private const val MAX_TRANSFER_LINKS = 6
        private const val CACHE_SIZE = 32
        private const val LOG_TAG = "PackTripSource"

        const val HERO_ORIGIN_ID = "va-rufino"
        const val HERO_DESTINATION_ID = "dela-rosa-pio-del-pilar"

        /** The hero pack, plus the optional OpenStreetMap and EDSA Carousel packs when bundled and valid; trips still work without them. */
        fun fromAssets(context: Context, netVotes: (String) -> Int = { 0 }): PackTripSource {
            val hero = CommutePack.parse(readAsset(context, HERO_ASSET_PATH))
            val pack = listOf(OSM_ASSET_PATH to "OpenStreetMap", CAROUSEL_ASSET_PATH to "EDSA Carousel")
                .fold(hero) { merged, (path, label) ->
                    try {
                        merged.merge(CommutePack.parse(readAsset(context, path)))
                    } catch (e: Exception) {
                        Log.w(LOG_TAG, "$label pack unavailable; skipping it", e)
                        merged
                    }
                }
            return PackTripSource(pack, netVotes)
        }

        private fun readAsset(context: Context, path: String): String =
            context.assets.open(path).bufferedReader().use { it.readText() }

        private fun edgeId(segment: PackSegment) = "${segment.routeId}#${segment.seq}"

        private fun haversineMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
            val dLat = Math.toRadians(lat2 - lat1)
            val dLng = Math.toRadians(lng2 - lng1)
            val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2) * sin(dLng / 2)
            return 2 * EARTH_RADIUS_M * atan2(sqrt(a), sqrt(1 - a))
        }
    }
}
